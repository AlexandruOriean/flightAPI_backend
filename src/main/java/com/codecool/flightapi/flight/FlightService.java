package com.codecool.flightapi.flight;

import com.codecool.flightapi.airline.AirlineService;
import com.codecool.flightapi.airport.AirportService;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;
    private final AirportService airportService;
    private final AirlineService airlineService;

    public List<FlightResponse> findAll() {
        return flightRepository.findAll().stream().map(flightMapper::toResponse).toList();
    }

    /**
     * Returns the flight with the given id.
     *
     * @throws ResourceNotFoundException if absent
     */
    public FlightResponse findById(Long id) {
        return flightMapper.toResponse(getById(id));
    }

    /**
     * Flights on {@code date} from one airport to another. Each airport is given either as its IATA code
     * ({@code OTP}) or as part of the airport or city name ({@code henri}, {@code London}), ignoring case.
     */
    public List<FlightResponse> search(String from, String to, LocalDate date) {
        return flightRepository
                .search(iataCode(from), containsPattern(from), iataCode(to), containsPattern(to), date)
                .stream()
                .map(flightMapper::toResponse)
                .toList();
    }

    /**
     * Creates a new flight.
     *
     * @throws ResourceNotFoundException if an airport or the airline does not exist
     */
    @Transactional
    public FlightResponse create(FlightRequest request) {
        Flight flight = flightMapper.toEntity(request);
        resolveReferences(request, flight);
        return flightMapper.toResponse(flightRepository.save(flight));
    }

    /** Replaces schedule, price, route and airline. */
    @Transactional
    public FlightResponse update(Long id, FlightRequest request) {
        Flight flight = getById(id);
        flightMapper.update(request, flight);
        resolveReferences(request, flight);
        return flightMapper.toResponse(flight);
    }

    @Transactional
    public void delete(Long id) {
        flightRepository.delete(getById(id));
    }

    private Flight getById(Long id) {
        return flightRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.byId("Flight", id));
    }

    private void resolveReferences(FlightRequest request, Flight flight) {
        flight.setDepartureAirport(airportService.getByIataCode(request.departureAirportCode()));
        flight.setArrivalAirport(airportService.getByIataCode(request.arrivalAirportCode()));
        flight.setAirline(airlineService.getByIcaoCode(request.airlineCode()));
    }

    private static String iataCode(String term) {
        return term.strip().toUpperCase(Locale.ROOT);
    }

    /** {@code %term%} in lower case, with LIKE wildcards in the term escaped so they match literally. */
    private static String containsPattern(String term) {
        String escaped = term.strip()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
