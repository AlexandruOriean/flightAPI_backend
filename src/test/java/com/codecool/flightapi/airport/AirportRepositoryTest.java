package com.codecool.flightapi.airport;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Airport queries against the schema and seed data from the real Flyway migrations (embedded H2). */
@DataJpaTest
class AirportRepositoryTest {

    @Autowired
    private AirportRepository airportRepository;

    @Test
    @DisplayName("IATA lookup loads the airport with its city in the same query")
    void findsAirportByIataCodeWithCity() {
        Airport airport = airportRepository.findByIataCode("OTP").orElseThrow();

        assertThat(airport.getName()).isEqualTo("Henri Coanda International Airport");
        assertThat(Hibernate.isInitialized(airport.getCity())).isTrue();
        assertThat(airport.getCity().getName()).isEqualTo("Bucuresti");
    }

    @Test
    @DisplayName("IATA lookup finds nothing for an unknown code")
    void unknownIataCodeIsEmpty() {
        assertThat(airportRepository.findByIataCode("XXX")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"CDG, Charles de Gaulle International Airport", "TSR, Timisoara Traian Vuia Airport"})
    @DisplayName("corrected seed IATA codes resolve to the right airport")
    void correctedIataCodesResolve(String iataCode, String airportName) {
        assertThat(airportRepository.findByIataCode(iataCode).orElseThrow().getName())
                .isEqualTo(airportName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHD", "TVT"})
    @DisplayName("the wrong IATA codes from the original seed data are gone")
    void wrongSeedIataCodesAreGone(String wrongCode) {
        assertThat(airportRepository.findByIataCode(wrongCode)).isEmpty();
    }

    @Test
    @DisplayName("airport names are returned for every seeded airport, alphabetically")
    void returnsAllAirportNames() {
        assertThat(airportRepository.findAllNames())
                .containsExactly(
                        "Charles de Gaulle International Airport",
                        "Henri Coanda International Airport",
                        "London Heathrow Airport",
                        "Timisoara Traian Vuia Airport");
    }
}
