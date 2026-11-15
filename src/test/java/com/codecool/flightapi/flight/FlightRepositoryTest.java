package com.codecool.flightapi.flight;

import static org.assertj.core.api.Assertions.assertThat;

import com.codecool.flightapi.airline.AirlineMapperImpl;
import com.codecool.flightapi.airline.AirlineService;
import com.codecool.flightapi.airport.AirportMapperImpl;
import com.codecool.flightapi.airport.AirportService;
import com.codecool.flightapi.city.CityMapperImpl;
import com.codecool.flightapi.city.CityService;
import com.codecool.flightapi.common.money.MoneyMapperImpl;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * The flight search query against the real Flyway schema and seed data (embedded H2), called through
 * {@link FlightService} so the IATA codes and LIKE patterns are built as in production.
 *
 * <p>Seeded flights on {@link #SEEDED_FLIGHT_DATE}: LHR→OTP, OTP→LHR, CDG→OTP, OTP→CDG, TSR→LHR.
 */
@DataJpaTest
@Import({
    FlightService.class,
    FlightMapperImpl.class,
    AirportService.class,
    AirportMapperImpl.class,
    AirlineService.class,
    AirlineMapperImpl.class,
    CityService.class,
    CityMapperImpl.class,
    MoneyMapperImpl.class
})
class FlightRepositoryTest {

    private static final LocalDate SEEDED_FLIGHT_DATE = LocalDate.of(2020, 11, 1);

    @Autowired
    private FlightService flightService;

    @Autowired
    private FlightRepository flightRepository;

    @Test
    @DisplayName("paged findAll and findById load the lazy airports, cities and airline in the same query")
    void entityGraphLoadsRelations() {
        List<Flight> flights =
                new ArrayList<>(flightRepository.findAll(PageRequest.of(0, 3)).getContent());
        flights.add(flightRepository.findById(1L).orElseThrow());

        assertThat(flights).hasSizeGreaterThan(1).allSatisfy(flight -> {
            assertThat(Hibernate.isInitialized(flight.getDepartureAirport())).isTrue();
            assertThat(Hibernate.isInitialized(flight.getDepartureAirport().getCity()))
                    .isTrue();
            assertThat(Hibernate.isInitialized(flight.getArrivalAirport().getCity()))
                    .isTrue();
            assertThat(Hibernate.isInitialized(flight.getAirline())).isTrue();
        });
    }

    @Test
    @DisplayName("flights are paged in departure order, with the total count")
    void findAllPagesInDepartureOrder() {
        // the five seed flights all leave at 09:30 local time, so the three from Romania (UTC+2) leave
        // first, then the one from Paris, then the one from London
        Page<FlightResponse> first = flightService.findAll(PageRequest.of(0, 2));
        Page<FlightResponse> last = flightService.findAll(PageRequest.of(2, 2));

        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(first.getContent())
                .extracting(flight -> flight.departureAirport().iataCode())
                .containsExactly("OTP", "OTP");
        assertThat(last.getContent())
                .extracting(flight -> flight.departureAirport().iataCode())
                .containsExactly("LHR");
    }

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
        List<FlightResponse> flights = flightService.search(from, to, SEEDED_FLIGHT_DATE);

        assertThat(flights).singleElement().satisfies(flight -> {
            assertThat(flight.departureAirport().iataCode()).isEqualTo("LHR");
            assertThat(flight.arrivalAirport().iataCode()).isEqualTo("OTP");
            assertThat(flight.arrivalAirport().city().cityName()).isEqualTo("Bucuresti");
            assertThat(flight.airline().name()).isEqualTo("Tarom");
        });
    }

    @Test
    @DisplayName("flight search respects the direction of travel")
    void searchRespectsDirection() {
        assertThat(flightService.search("OTP", "LHR", SEEDED_FLIGHT_DATE))
                .singleElement()
                .satisfies(flight -> assertThat(flight.airline().name()).isEqualTo("British Airways"));
    }

    @Test
    @DisplayName("flight search returns every flight arriving at an airport from matching departures")
    void searchReturnsAllMatches() {
        // 'airport' is part of every seeded airport name
        assertThat(flightService.search("airport", "OTP", SEEDED_FLIGHT_DATE))
                .extracting(flight -> flight.departureAirport().iataCode())
                .containsExactlyInAnyOrder("LHR", "CDG");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"%%%, %%%", "___, ___", "'%%%', OTP"})
    @DisplayName("LIKE wildcards in the search terms match literally, not everything")
    void wildcardsAreEscaped(String from, String to) {
        assertThat(flightService.search(from, to, SEEDED_FLIGHT_DATE)).isEmpty();
    }

    @Test
    @DisplayName("the search query returns at most the limit, earliest departures first")
    void searchQueryAppliesTheLimit() {
        Instant dayStart = Instant.parse("2020-11-01T00:00:00Z");
        // 'airport' matches every seeded airport, so all five seed flights fall in the window
        List<Flight> flights = flightRepository.search(
                "AIRPORT",
                "%airport%",
                "AIRPORT",
                "%airport%",
                dayStart,
                dayStart.plus(Duration.ofDays(1)),
                Limit.of(2));

        assertThat(flights)
                .extracting(flight -> flight.getDepartureAirport().getAirportIataCode())
                .containsExactly("OTP", "OTP");
    }

    @Test
    @DisplayName("flight search returns nothing for a date without flights")
    void flightSearchOnOtherDateIsEmpty() {
        assertThat(flightService.search("London", "Henri", SEEDED_FLIGHT_DATE.plusDays(1)))
                .isEmpty();
    }
}
