package com.codecool.flightapi.flight;

import com.codecool.flightapi.aircraft.AircraftTypeService;
import com.codecool.flightapi.airline.AirlineService;
import com.codecool.flightapi.airport.AirportService;
import com.codecool.flightapi.common.error.InvalidRequestException;
import com.codecool.flightapi.common.error.PreconditionFailedException;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import com.codecool.flightapi.common.web.ETags.IfMatch;
import com.codecool.flightapi.common.web.Pages;
import java.time.LocalDate;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightService {

    private static final Sort ORDER = Sort.by("departureAt", "id");

    /**
     * Shortest airport or city name term a search accepts; IATA codes have exactly this length. Shorter terms
     * would match most airports and make the name patterns expensive.
     */
    static final int MIN_SEARCH_TERM_LENGTH = 3;

    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;
    private final AirportService airportService;
    private final AirlineService airlineService;
    private final AircraftTypeService aircraftTypeService;

    /** One page of flights, ordered by departure time. */
    public Page<FlightResponse> findAll(Pageable pageable) {
        return flightRepository.findAll(Pages.ordered(pageable, ORDER)).map(flightMapper::toResponse);
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
     * One page of flights from one airport to another departing on {@code date}, in the departure airport's
     * local time, ordered by departure time. Each airport is given either as its IATA code ({@code OTP}) or as
     * part of the airport or city name ({@code henri}, {@code London}), ignoring case.
     *
     * @throws InvalidRequestException if a term is shorter than {@value #MIN_SEARCH_TERM_LENGTH} characters
     */
    public Page<FlightResponse> search(String from, String to, LocalDate date, Pageable pageable) {
        requireSearchTerm(from);
        requireSearchTerm(to);
        return flightRepository
                .search(
                        iataCode(from),
                        containsPattern(from),
                        iataCode(to),
                        containsPattern(to),
                        date,
                        Pages.ordered(pageable, ORDER))
                .map(flightMapper::toResponse);
    }

    /**
     * Creates a new flight.
     *
     * @throws ResourceNotFoundException if an airport, the airline or the aircraft type does not exist
     */
    @Transactional
    public FlightResponse create(FlightRequest request) {
        Flight flight = flightMapper.toEntity(request);
        resolveReferences(request, flight);
        return flightMapper.toResponse(flightRepository.save(flight));
    }

    /**
     * Replaces times, price, route, airline and aircraft type (none if the request has no code).
     *
     * @throws PreconditionFailedException if {@code ifMatch} names an older version
     */
    @Transactional
    public FlightResponse update(Long id, FlightRequest request, IfMatch ifMatch) {
        Flight flight = getById(id);
        ifMatch.requireMatches(flight.getVersion());
        flightMapper.update(request, flight);
        flight.reschedule(request.departure().toInstant(), request.arrival().toInstant());
        resolveReferences(request, flight);
        // write now, so the response carries the new version
        flightRepository.flush();
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
        flight.setAircraftType(
                request.aircraftTypeCode() == null
                        ? null
                        : aircraftTypeService.getByIcaoCode(request.aircraftTypeCode()));
    }

    private static void requireSearchTerm(String term) {
        if (term.strip().length() < MIN_SEARCH_TERM_LENGTH) {
            throw new InvalidRequestException("Search terms must be at least " + MIN_SEARCH_TERM_LENGTH
                    + " characters: an IATA code or part of an airport or city name.");
        }
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
