package db.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link V14__Aircraft_types} on a fresh H2 database migrated up to V13, with and without admin-added airplanes. */
class V14AircraftTypesTest {

    @Test
    @DisplayName("only the V2 seed airplanes: they are replaced by the aircraft types and the table is dropped")
    void replacesSeedAirplanes() throws SQLException {
        String url = database("v14-seed-only");
        migrate(url, "13");

        migrate(url, "14");

        assertThat(count(url, "SELECT COUNT(*) FROM aircraft_type")).isEqualTo(6);
        assertThat(count(url, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'AIRPLANE'"))
                .isZero();
    }

    @Test
    @DisplayName("an airplane added by an admin stops the migration instead of being dropped")
    void stopsOnAdminAddedAirplanes() throws SQLException {
        String url = database("v14-admin-added");
        migrate(url, "13");
        execute(
                url,
                "INSERT INTO airplane (manufacturer, model, number_of_seats, speed) VALUES ('Embraer', 'E195', 120, 830)");

        assertThatThrownBy(() -> migrate(url, "14"))
                .rootCause()
                .hasMessageContaining("airplanes added after the seed data need an ICAO type code")
                .hasMessageContaining("Embraer|E195|120|830");
        assertThat(count(url, "SELECT COUNT(*) FROM airplane")).isEqualTo(8);
    }

    private static String database(String name) {
        return "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1";
    }

    private static void migrate(String url, String target) {
        Flyway.configure()
                .dataSource(url, "sa", "")
                .target(MigrationVersion.fromVersion(target))
                .load()
                .migrate();
    }

    private static void execute(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static long count(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
