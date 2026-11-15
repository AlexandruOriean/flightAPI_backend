package com.codecool.flightapi.flight;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * The flight search query against the real Flyway schema and seed data (embedded H2), called through
 * {@link FlightService} so the IATA codes and LIKE patterns are built as in production.
 *
 * <p>Seeded flights on {@link #SEEDED_FLIGHT_DATE}: LHR→OTP, OTP→LHR, CDG→OTP, OTP→CDG, TSR→LHR.
 */
@DataJpaTest
@Import(FlightService.class)
class FlightRepositoryTest {

    private static final LocalDate SEEDED_FLIGHT_DATE = LocalDate.of(2020, 11, 1);

    @Autowired
    private FlightService flightService;

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "London, Henri", // partial airport names, as the frontend sends them
        "LHR, OTP", // IATA codes
        "lhr, otp", // IATA codes, any case
        "london, bucuresti", // city names
        "' heathrow ', COANDA", // surrounding whitespace, case-insensitive name
    })
    @DisplayName("flight search matches each airport by IATA code, airport name or city name")
    void searchMatchesIataCodeOrName(String from, String to) {
        List<Flight> flights = flightService.search(from, to, SEEDED_FLIGHT_DATE);

        assertThat(flights).singleElement().satisfies(flight -> {
            assertThat(flight.getDepartureAirport().getAirportIataCode()).isEqualTo("LHR");
            assertThat(flight.getArrivalAirport().getAirportIataCode()).isEqualTo("OTP");
            assertThat(flight.getArrivalAirport().getCity().getCityName()).isEqualTo("Bucuresti");
            assertThat(flight.getAirline().getName()).isEqualTo("Tarom");
        });
    }

    @Test
    @DisplayName("flight search respects the direction of travel")
    void searchRespectsDirection() {
        assertThat(flightService.search("OTP", "LHR", SEEDED_FLIGHT_DATE))
                .singleElement()
                .satisfies(flight -> assertThat(flight.getAirline().getName()).isEqualTo("British Airways"));
    }

    @Test
    @DisplayName("flight search returns every flight arriving at an airport from matching departures")
    void searchReturnsAllMatches() {
        // 'airport' is part of every seeded airport name
        assertThat(flightService.search("airport", "OTP", SEEDED_FLIGHT_DATE))
                .extracting(flight -> flight.getDepartureAirport().getAirportIataCode())
                .containsExactlyInAnyOrder("LHR", "CDG");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"%, %", "_, _", "'%', OTP"})
    @DisplayName("LIKE wildcards in the search terms match literally, not everything")
    void wildcardsAreEscaped(String from, String to) {
        assertThat(flightService.search(from, to, SEEDED_FLIGHT_DATE)).isEmpty();
    }

    @Test
    @DisplayName("flight search returns nothing for a date without flights")
    void flightSearchOnOtherDateIsEmpty() {
        assertThat(flightService.search("London", "Henri", SEEDED_FLIGHT_DATE.plusDays(1)))
                .isEmpty();
    }
}
