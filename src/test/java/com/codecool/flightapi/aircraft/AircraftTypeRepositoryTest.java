package com.codecool.flightapi.aircraft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/** Aircraft types against the real Flyway schema and seed data (embedded H2). */
@DataJpaTest
class AircraftTypeRepositoryTest {

    @Autowired
    private AircraftTypeRepository aircraftTypeRepository;

    @Test
    @DisplayName("V14 seeds the aircraft types of the seeded airlines, looked up by ICAO code")
    void seedTypesAreFoundByIcaoCode() {
        assertThat(aircraftTypeRepository.findAll())
                .extracting(AircraftType::getIcaoCode)
                .containsExactlyInAnyOrder("A320", "A21N", "A333", "AT76", "B738", "B77W");
        assertThat(aircraftTypeRepository.findByIcaoCode("B738"))
                .hasValueSatisfying(type -> assertThat(type.getModel()).isEqualTo("737-800"));
    }

    @Test
    @DisplayName("ICAO codes are unique")
    void icaoCodesAreUnique() {
        assertThatThrownBy(() -> aircraftTypeRepository.saveAndFlush(
                        new AircraftType(null, "A320", "Airbus", "A320 copy", 840, 180)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "speed={0}, seats={1}")
    @CsvSource({"0, 180", "840, 0"})
    @DisplayName("the database rejects a non-positive cruise speed or seat count")
    void databaseRejectsNonPositiveValues(int cruiseSpeedKmh, int typicalSeats) {
        assertThatThrownBy(() -> aircraftTypeRepository.saveAndFlush(
                        new AircraftType(null, "TEST", "Test", "Test", cruiseSpeedKmh, typicalSeats)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
