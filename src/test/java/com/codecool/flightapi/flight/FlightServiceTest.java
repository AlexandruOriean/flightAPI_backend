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
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Revives the flight search test that was commented out in FlightApiProjectApplicationTests, as unit tests. */
@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    private static final LocalDate DATE = LocalDate.of(2021, 1, 15);

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirportService airportService;

    @Mock
    private AirlineService airlineService;

    private FlightService flightService;

    private final Airport otopeni = new Airport(1L, "OTP", "Otopeni", new City(12L, "Bucuresti"));
    private final Airport heathrow = new Airport(2L, "LHR", "Heathrow", new City(21L, "London"));
    private final Airline tarom = new Airline(10000L, "Tarom", "ROT");

    @BeforeEach
    void setUp() {
        FlightMapper mapper =
                new FlightMapperImpl(new AirportMapperImpl(new CityMapperImpl()), new AirlineMapperImpl());
        flightService = new FlightService(flightRepository, mapper, airportService, airlineService);
    }

    @Test
    void searchReturnsTheFlightsFoundForRouteAndDate() {
        List<Flight> flights = List.of(
                flight(1L, LocalTime.of(9, 20), LocalTime.of(11, 20)),
                flight(2L, LocalTime.of(10, 20), LocalTime.of(12, 20)),
                flight(3L, LocalTime.of(10, 20), LocalTime.of(13, 20)));
        when(flightRepository.search("OTOPENI", "%otopeni%", "HEATHROW", "%heathrow%", DATE))
                .thenReturn(flights);

        assertThat(flightService.search("Otopeni", "Heathrow", DATE)).hasSize(3).allSatisfy(flight -> {
            assertThat(flight.departureAirport().iataCode()).isEqualTo("OTP");
            assertThat(flight.arrivalAirport().city().cityName()).isEqualTo("London");
            assertThat(flight.airline().icaoCode()).isEqualTo("ROT");
            assertThat(flight.departureDate()).isEqualTo(DATE);
        });
    }

    @Test
    void searchReturnsEmptyListWhenNothingMatches() {
        when(flightRepository.search("OTOPENI", "%otopeni%", "HEATHROW", "%heathrow%", DATE))
                .thenReturn(List.of());

        assertThat(flightService.search("Otopeni", "Heathrow", DATE)).isEmpty();
    }

    @Test
    void searchEscapesLikeWildcardsAndNormalizesTerms() {
        flightService.search(" 50%_off\\ ", "lhr", DATE);

        verify(flightRepository).search("50%_OFF\\", "%50\\%\\_off\\\\%", "LHR", "%lhr%", DATE);
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
        assertThat(created.price()).isEqualTo(250L);
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
    void updateReplacesScheduleRouteAndPrice() {
        Flight existing = flight(1L, LocalTime.of(9, 20), LocalTime.of(11, 20));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(airportService.getByIataCode("LHR")).thenReturn(heathrow);
        when(airportService.getByIataCode("OTP")).thenReturn(otopeni);
        when(airlineService.getByIcaoCode("ROT")).thenReturn(tarom);

        FlightResponse updated = flightService.update(1L, request("LHR", "OTP"));

        assertThat(updated.id()).isEqualTo(1L);
        assertThat(updated.departureDate()).isEqualTo(DATE.plusDays(1));
        assertThat(updated.departureTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(updated.arrivalTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(updated.price()).isEqualTo(250L);
        assertThat(existing.getDepartureAirport()).isEqualTo(heathrow);
        assertThat(existing.getArrivalAirport()).isEqualTo(otopeni);
    }

    private static FlightRequest request(String from, String to) {
        return new FlightRequest(from, to, "ROT", DATE.plusDays(1), LocalTime.of(7, 0), LocalTime.of(9, 0), 250L);
    }

    private Flight flight(Long id, LocalTime departure, LocalTime arrival) {
        return new Flight(id, DATE, 100L, departure, arrival, otopeni, heathrow, tarom);
    }
}
