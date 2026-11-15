package com.codecool.flightapi.flight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codecool.flightapi.aircraft.AircraftTypeMapperImpl;
import com.codecool.flightapi.aircraft.AircraftTypeService;
import com.codecool.flightapi.airline.AirlineMapperImpl;
import com.codecool.flightapi.airline.AirlineService;
import com.codecool.flightapi.airport.Airport;
import com.codecool.flightapi.airport.AirportMapperImpl;
import com.codecool.flightapi.airport.AirportRequest;
import com.codecool.flightapi.airport.AirportService;
import com.codecool.flightapi.city.CityMapperImpl;
import com.codecool.flightapi.city.CityService;
import com.codecool.flightapi.common.money.Money;
import com.codecool.flightapi.common.money.MoneyDto;
import com.codecool.flightapi.common.money.MoneyMapperImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

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
    AircraftTypeService.class,
    AircraftTypeMapperImpl.class,
    CityService.class,
    CityMapperImpl.class,
    MoneyMapperImpl.class,
    DepartureLocalDateUpdater.class
})
class FlightRepositoryTest {

    private static final LocalDate SEEDED_FLIGHT_DATE = LocalDate.of(2020, 11, 1);

    @Autowired
    private FlightService flightService;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AirportService airportService;

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
            assertThat(Hibernate.isInitialized(flight.getAircraftType())).isTrue();
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
        List<FlightResponse> flights = search(from, to, SEEDED_FLIGHT_DATE);

        assertThat(flights).singleElement().satisfies(flight -> {
            assertThat(flight.departureAirport().iataCode()).isEqualTo("LHR");
            assertThat(flight.arrivalAirport().iataCode()).isEqualTo("OTP");
            assertThat(flight.arrivalAirport().city().name()).isEqualTo("Bucuresti");
            assertThat(flight.airline().name()).isEqualTo("Tarom");
            // V15 gave the seed flights an aircraft type; the search fetches it with the flight
            assertThat(flight.aircraftType().icaoCode()).isEqualTo("B738");
        });
    }

    @Test
    @DisplayName("flight search respects the direction of travel")
    void searchRespectsDirection() {
        assertThat(search("OTP", "LHR", SEEDED_FLIGHT_DATE))
                .singleElement()
                .satisfies(flight -> assertThat(flight.airline().name()).isEqualTo("British Airways"));
    }

    @Test
    @DisplayName("flight search returns every flight arriving at an airport from matching departures")
    void searchReturnsAllMatches() {
        // 'airport' is part of every seeded airport name
        assertThat(search("airport", "OTP", SEEDED_FLIGHT_DATE))
                .extracting(flight -> flight.departureAirport().iataCode())
                .containsExactlyInAnyOrder("LHR", "CDG");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"%%%, %%%", "___, ___", "'%%%', OTP"})
    @DisplayName("LIKE wildcards in the search terms match literally, not everything")
    void wildcardsAreEscaped(String from, String to) {
        assertThat(search(from, to, SEEDED_FLIGHT_DATE)).isEmpty();
    }

    @Test
    @DisplayName("the search is paged in the database, earliest departures first")
    void searchIsPaged() {
        // 'airport' matches every seeded airport, so all five seed flights match
        Page<FlightResponse> first =
                flightService.search("airport", "airport", SEEDED_FLIGHT_DATE, PageRequest.of(0, 2));
        Page<FlightResponse> last =
                flightService.search("airport", "airport", SEEDED_FLIGHT_DATE, PageRequest.of(2, 2));

        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.getContent())
                .extracting(flight -> flight.departureAirport().iataCode())
                .containsExactly("OTP", "OTP");
        assertThat(last.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("V16 gave every seeded flight its departure date in the departure airport's time zone")
    void seededFlightsHaveTheirLocalDepartureDate() {
        assertThat(flightRepository.findAll())
                .isNotEmpty()
                .allSatisfy(flight -> assertThat(flight.getDepartureLocalDate())
                        .isEqualTo(flight.getDepartureAt()
                                .atZone(flight.getDepartureAirport().getTimeZone())
                                .toLocalDate()));
    }

    @Test
    @DisplayName("a flight belongs to the day it departs in local time, not in UTC")
    void searchUsesTheLocalDepartureDate() {
        // 23:30 UTC on 1 November is 01:30 on 2 November in Bucharest (UTC+2)
        FlightResponse late = flightService.create(new FlightRequest(
                "OTP",
                "LHR",
                "ROT",
                OffsetDateTime.parse("2020-11-01T23:30:00Z"),
                OffsetDateTime.parse("2020-11-02T03:00:00Z"),
                new MoneyDto(new BigDecimal("50.00"), "EUR"),
                null));

        assertThat(search("OTP", "LHR", SEEDED_FLIGHT_DATE.plusDays(1)))
                .extracting(FlightResponse::id)
                .containsExactly(late.id());
        assertThat(search("OTP", "LHR", SEEDED_FLIGHT_DATE))
                .extracting(FlightResponse::id)
                .doesNotContain(late.id());
    }

    @Test
    @DisplayName("moving an airport to another time zone recomputes its flights' local departure dates")
    void airportTimeZoneChangeMovesItsFlights() {
        Flight fromOtopeni = flightRepository.findAll().stream()
                .filter(flight -> flight.getDepartureAirport().getIataCode().equals("OTP"))
                .findFirst()
                .orElseThrow();
        Airport otopeni = fromOtopeni.getDepartureAirport();
        // a zone far enough from UTC that the flight's departure falls on another day there
        ZoneId farAway = fromOtopeni.getDepartureAt().atZone(ZoneOffset.UTC).getHour() < 11
                ? ZoneId.of("Pacific/Pago_Pago") // UTC-11
                : ZoneId.of("Pacific/Kiritimati"); // UTC+14
        LocalDate newDate = fromOtopeni.getDepartureAt().atZone(farAway).toLocalDate();
        assertThat(newDate).isNotEqualTo(SEEDED_FLIGHT_DATE);

        airportService.update(
                otopeni.getId(),
                new AirportRequest("OTP", otopeni.getName(), otopeni.getCity().getName(), farAway.getId()));

        assertThat(search("OTP", "airport", newDate))
                .extracting(FlightResponse::id)
                .contains(fromOtopeni.getId());
        assertThat(search("OTP", "airport", SEEDED_FLIGHT_DATE))
                .extracting(FlightResponse::id)
                .doesNotContain(fromOtopeni.getId());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "arrival before departure, 2020-11-01T10:00:00Z, 2020-11-01T09:00:00Z, LHR, 35.00",
        "same departure and arrival airport, 2020-11-01T09:00:00Z, 2020-11-01T10:00:00Z, OTP, 35.00",
        "negative price, 2020-11-01T09:00:00Z, 2020-11-01T10:00:00Z, LHR, -1.00"
    })
    @DisplayName("the database rejects flights that break the API's validation rules")
    void databaseRejectsInvalidFlights(
            String rule, Instant departure, Instant arrival, String arrivalIataCode, BigDecimal price) {
        Flight seeded = flightRepository.findById(1L).orElseThrow(); // LHR -> OTP
        Airport otopeni = seeded.getArrivalAirport();
        Airport arrivalAirport = arrivalIataCode.equals("OTP") ? otopeni : seeded.getDepartureAirport();
        Flight invalid = new Flight(
                null,
                seeded.getDepartureAt(),
                seeded.getArrivalAt(),
                new Money(price, Currency.getInstance("EUR")),
                otopeni,
                arrivalAirport,
                seeded.getAirline());
        // past Flight#reschedule, as a writer that bypasses the entity (SQL, another service) could
        ReflectionTestUtils.setField(invalid, "departureAt", departure);
        ReflectionTestUtils.setField(invalid, "arrivalAt", arrival);

        assertThatThrownBy(() -> flightRepository.saveAndFlush(invalid))
                .as(rule)
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("flight search returns nothing for a date without flights")
    void flightSearchOnOtherDateIsEmpty() {
        assertThat(search("London", "Henri", SEEDED_FLIGHT_DATE.plusDays(1))).isEmpty();
    }

    private List<FlightResponse> search(String from, String to, LocalDate date) {
        return flightService.search(from, to, date, PageRequest.of(0, 20)).getContent();
    }
}
