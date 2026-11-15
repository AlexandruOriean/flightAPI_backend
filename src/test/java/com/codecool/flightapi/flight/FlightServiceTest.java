package com.codecool.flightapi.flight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codecool.flightapi.airline.Airline;
import com.codecool.flightapi.airline.AirlineMapperImpl;
import com.codecool.flightapi.airline.AirlineService;
import com.codecool.flightapi.airport.Airport;
import com.codecool.flightapi.airport.AirportMapperImpl;
import com.codecool.flightapi.airport.AirportService;
import com.codecool.flightapi.city.City;
import com.codecool.flightapi.city.CityMapperImpl;
import com.codecool.flightapi.common.error.InvalidRequestException;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import com.codecool.flightapi.common.money.Money;
import com.codecool.flightapi.common.money.MoneyDto;
import com.codecool.flightapi.common.money.MoneyMapperImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

/** Revives the flight search test that was commented out in FlightApiProjectApplicationTests, as unit tests. */
@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    private static final LocalDate DATE = LocalDate.of(2021, 1, 15);

    /** The UTC window that contains every local date {@link #DATE}: from UTC+14:00 to UTC-12:00. */
    private static final Instant WINDOW_START = Instant.parse("2021-01-14T10:00:00Z");

    private static final Instant WINDOW_END = Instant.parse("2021-01-16T12:00:00Z");

    /** One row more than a search may return, to detect a cut-off result. */
    private static final Limit SEARCH_LIMIT = Limit.of(FlightService.MAX_SEARCH_ROWS + 1);

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirportService airportService;

    @Mock
    private AirlineService airlineService;

    private FlightService flightService;

    private final Airport otopeni =
            new Airport(1L, "OTP", "Otopeni", new City(12L, "Bucuresti"), ZoneId.of("Europe/Bucharest"));
    private final Airport heathrow =
            new Airport(2L, "LHR", "Heathrow", new City(21L, "London"), ZoneId.of("Europe/London"));
    private final Airline tarom = new Airline(10000L, "Tarom", "ROT");

    @BeforeEach
    void setUp() {
        FlightMapper mapper = new FlightMapperImpl(
                new AirportMapperImpl(new CityMapperImpl()), new AirlineMapperImpl(), new MoneyMapperImpl());
        flightService = new FlightService(flightRepository, mapper, airportService, airlineService);
    }

    @Test
    void searchShowsTimesInTheLocalTimeOfEachAirport() {
        when(flightRepository.search(
                        "OTOPENI", "%otopeni%", "HEATHROW", "%heathrow%", WINDOW_START, WINDOW_END, SEARCH_LIMIT))
                .thenReturn(List.of(flight(1L, "2021-01-15T07:20:00Z", "2021-01-15T10:50:00Z")));

        assertThat(flightService.search("Otopeni", "Heathrow", DATE))
                .singleElement()
                .satisfies(flight -> {
                    assertThat(flight.departure()).isEqualTo(OffsetDateTime.parse("2021-01-15T09:20:00+02:00"));
                    assertThat(flight.arrival()).isEqualTo(OffsetDateTime.parse("2021-01-15T10:50:00Z"));
                    assertThat(flight.price()).isEqualTo(new MoneyDto(new BigDecimal("100.00"), "RON"));
                    assertThat(flight.departureAirport().timeZone()).isEqualTo("Europe/Bucharest");
                    assertThat(flight.airline().icaoCode()).isEqualTo("ROT");
                });
    }

    @Test
    void searchUsesTheLocalDateAtTheDepartureAirport() {
        when(flightRepository.search("OTP", "%otp%", "LHR", "%lhr%", WINDOW_START, WINDOW_END, SEARCH_LIMIT))
                .thenReturn(List.of(
                        // 23:30 UTC on the 14th is already 01:30 on the 15th in Bucharest
                        flight(1L, "2021-01-14T23:30:00Z", "2021-01-15T03:00:00Z"),
                        // 22:30 UTC on the 15th is 00:30 on the 16th in Bucharest
                        flight(2L, "2021-01-15T22:30:00Z", "2021-01-16T02:00:00Z")));

        assertThat(flightService.search("OTP", "LHR", DATE))
                .extracting(FlightResponse::id)
                .containsExactly(1L);
    }

    @Test
    void searchEscapesLikeWildcardsAndNormalizesTerms() {
        flightService.search(" 50%_off\\ ", "lhr", DATE);

        verify(flightRepository)
                .search("50%_OFF\\", "%50\\%\\_off\\\\%", "LHR", "%lhr%", WINDOW_START, WINDOW_END, SEARCH_LIMIT);
    }

    @Test
    void searchRejectsTermsShorterThanThreeCharactersWithoutQuerying() {
        assertThatThrownBy(() -> flightService.search(" ab ", "LHR", DATE))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at least 3 characters");
        assertThatThrownBy(() -> flightService.search("OTP", "a", DATE)).isInstanceOf(InvalidRequestException.class);

        verify(flightRepository, never()).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchRejectsTermsThatMatchTooManyFlights() {
        Flight flight = flight(1L, "2021-01-15T07:20:00Z", "2021-01-15T10:50:00Z");
        when(flightRepository.search("AIR", "%air%", "LHR", "%lhr%", WINDOW_START, WINDOW_END, SEARCH_LIMIT))
                .thenReturn(Collections.nCopies(FlightService.MAX_SEARCH_ROWS + 1, flight));

        assertThatThrownBy(() -> flightService.search("air", "LHR", DATE))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("too many flights");
    }

    @Test
    void searchReturnsAResultOfExactlyTheMaximumSize() {
        Flight flight = flight(1L, "2021-01-15T07:20:00Z", "2021-01-15T10:50:00Z");
        when(flightRepository.search("AIR", "%air%", "LHR", "%lhr%", WINDOW_START, WINDOW_END, SEARCH_LIMIT))
                .thenReturn(Collections.nCopies(FlightService.MAX_SEARCH_ROWS, flight));

        assertThat(flightService.search("air", "LHR", DATE)).hasSize(FlightService.MAX_SEARCH_ROWS);
    }

    @Test
    void findByIdThrowsNotFoundForUnknownId() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Flight with id '99' was not found");
    }

    @Test
    void createResolvesAirportsAndAirlineFromTheirCodes() {
        when(airportService.getByIataCode("OTP")).thenReturn(otopeni);
        when(airportService.getByIataCode("LHR")).thenReturn(heathrow);
        when(airlineService.getByIcaoCode("ROT")).thenReturn(tarom);
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightResponse created = flightService.create(request("OTP", "LHR"));

        assertThat(created.departureAirport().id()).isEqualTo(1L);
        assertThat(created.arrivalAirport().id()).isEqualTo(2L);
        assertThat(created.airline().id()).isEqualTo(10000L);
        assertThat(created.price()).isEqualTo(new MoneyDto(new BigDecimal("250.50"), "EUR"));
        // stored as an instant, shown in the departure airport's zone whatever offset the client sent
        assertThat(created.departure()).isEqualTo(OffsetDateTime.parse("2021-01-16T07:00:00+02:00"));
    }

    @Test
    void createWithUnknownAirportSavesNothing() {
        when(airportService.getByIataCode("XXX"))
                .thenThrow(new ResourceNotFoundException("Airport", "IATA code", "XXX"));

        assertThatThrownBy(() -> flightService.create(request("XXX", "LHR")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void updateReplacesTimesRouteAndPrice() {
        Flight existing = flight(1L, "2021-01-15T07:20:00Z", "2021-01-15T10:50:00Z");
        when(flightRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(airportService.getByIataCode("LHR")).thenReturn(heathrow);
        when(airportService.getByIataCode("OTP")).thenReturn(otopeni);
        when(airlineService.getByIcaoCode("ROT")).thenReturn(tarom);

        FlightResponse updated = flightService.update(1L, request("LHR", "OTP"));

        assertThat(updated.id()).isEqualTo(1L);
        assertThat(existing.getDepartureAt()).isEqualTo(Instant.parse("2021-01-16T05:00:00Z"));
        assertThat(existing.getArrivalAt()).isEqualTo(Instant.parse("2021-01-16T08:30:00Z"));
        assertThat(updated.departure()).isEqualTo(OffsetDateTime.parse("2021-01-16T05:00:00Z"));
        assertThat(updated.price()).isEqualTo(new MoneyDto(new BigDecimal("250.50"), "EUR"));
        assertThat(existing.getDepartureAirport()).isEqualTo(heathrow);
        assertThat(existing.getArrivalAirport()).isEqualTo(otopeni);
    }

    private static FlightRequest request(String from, String to) {
        return new FlightRequest(
                from,
                to,
                "ROT",
                OffsetDateTime.parse("2021-01-16T06:00:00+01:00"),
                OffsetDateTime.parse("2021-01-16T08:30:00Z"),
                new MoneyDto(new BigDecimal("250.50"), "EUR"));
    }

    private Flight flight(Long id, String departure, String arrival) {
        return new Flight(
                id,
                Instant.parse(departure),
                Instant.parse(arrival),
                new Money(new BigDecimal("100.00"), Currency.getInstance("RON")),
                otopeni,
                heathrow,
                tarom);
    }
}
