package com.codecool.flightapi.airplane;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/** Airplanes against the real Flyway schema (embedded H2). */
@DataJpaTest
class AirplaneRepositoryTest {

    @Autowired
    private AirplaneRepository airplaneRepository;

    @ParameterizedTest(name = "seats={0}, speed={1}")
    @CsvSource({"0, 800", "150, 0"})
    void databaseRejectsAirplanesWithoutSeatsOrSpeed(long seats, double speed) {
        assertThatThrownBy(() -> airplaneRepository.saveAndFlush(new Airplane(null, "Airbus", "A320", seats, speed)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
