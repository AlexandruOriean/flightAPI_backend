package com.codecool.flightapi.city;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Cities against the real Flyway schema (embedded H2), committed in separate transactions. */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CityRepositoryTest {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long cityId;

    @AfterEach
    void deleteCity() {
        if (cityId != null) {
            cityRepository.deleteById(cityId);
        }
    }

    @Test
    @DisplayName("city names are unique regardless of case and surrounding whitespace")
    void namesAreUniqueIgnoringCase() {
        // 'London' is seeded
        assertThatThrownBy(() -> cityRepository.save(new City(null, " LONDON ")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("lookup by name key finds the city whatever the case")
    void findsCityByNameKey() {
        assertThat(cityRepository.findByCityNameKey(City.key(" lONdon ")))
                .hasValueSatisfying(city -> assertThat(city.getCityName()).isEqualTo("London"));
    }

    @Test
    @DisplayName("renaming a city keeps its name key in step")
    void renameUpdatesKey() {
        cityId = cityRepository.save(new City(null, "Cluj")).getCityId();
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        tx -> cityRepository.findById(cityId).orElseThrow().setCityName("Cluj-Napoca"));

        assertThat(cityRepository.findByCityNameKey("cluj-napoca")).isPresent();
        assertThat(cityRepository.findByCityNameKey("cluj")).isEmpty();
    }

    @Test
    @DisplayName("of two overlapping updates of one city, the second fails instead of overwriting the first")
    void concurrentUpdateFails() {
        cityId = cityRepository.save(new City(null, "Cluj")).getCityId();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(first -> {
                    City stale = cityRepository.findById(cityId).orElseThrow();
                    // a second request updates and commits the same city in between
                    new TransactionTemplate(
                                    transactionManager,
                                    new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW))
                            .executeWithoutResult(second -> cityRepository
                                    .findById(cityId)
                                    .orElseThrow()
                                    .setCityName("Cluj-Napoca"));
                    stale.setCityName("Kolozsvar");
                }))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        City city = cityRepository.findById(cityId).orElseThrow();
        assertThat(city.getCityName()).isEqualTo("Cluj-Napoca");
        assertThat(city.getVersion()).isEqualTo(1);
    }
}
