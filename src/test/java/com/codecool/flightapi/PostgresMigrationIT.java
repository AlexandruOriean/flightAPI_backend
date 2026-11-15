package com.codecool.flightapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codecool.flightapi.airport.AirportService;
import com.codecool.flightapi.flight.FlightService;
import com.codecool.flightapi.user.RoleName;
import com.codecool.flightapi.user.RoleRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDate;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Boots the full application against a real PostgreSQL (the production database) to prove that
 * the Flyway migrations apply cleanly, that Hibernate's schema validation passes, and that the
 * native queries work on PostgreSQL. Skipped automatically when Docker is not available.
 */
@SpringBootTest(
        properties =
                // Test-only key (64 random bytes, Base64). Never use outside tests.
                "app.jwt.secret=dGVzdC1vbmx5LWtleS1kby1ub3QtdXNlLWluLXByb2R1Y3Rpb24tMDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3A=")
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AirportService airportService;

    @Autowired
    private FlightService flightService;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("all migrations are applied on PostgreSQL")
    void migrationsAreApplied() {
        assertThat(flyway.info().pending()).isEmpty();
        MigrationInfo[] all = flyway.info().all();
        // the newest migration on the classpath is the one applied last, whatever its number
        assertThat(flyway.info().current().getVersion()).isEqualTo(all[all.length - 1].getVersion());
        assertThat(all)
                .allSatisfy(migration -> assertThat(migration.getState().isApplied())
                        .as("V%s applied", migration.getVersion())
                        .isTrue());
    }

    @Test
    @DisplayName("seed data and native queries work on PostgreSQL")
    void nativeQueriesWorkOnPostgres() {
        assertThat(airportService.findByIataCode("otp").name()).isEqualTo("Henri Coanda International Airport");
        assertThat(flightService.search("London", "Henri", LocalDate.of(2020, 11, 1), PageRequest.of(0, 20)))
                .hasSize(1);
        assertThat(roleRepository.findByName(RoleName.ROLE_ADMIN)).isPresent();
    }

    @Test
    @DisplayName("transactions have a default deadline that also cancels slow queries on PostgreSQL")
    void slowQueryIsCancelledAtTheTransactionTimeout() {
        assertThat(((AbstractPlatformTransactionManager) transactionManager).getDefaultTimeout())
                .isEqualTo(10);

        TransactionTemplate oneSecond = new TransactionTemplate(transactionManager);
        oneSecond.setTimeout(1);
        long start = System.nanoTime();
        assertThatThrownBy(() -> oneSecond.executeWithoutResult(tx ->
                        entityManager.createNativeQuery("SELECT pg_sleep(5)").getSingleResult()))
                .hasMessageContaining("canceling statement");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(4));
    }
}
