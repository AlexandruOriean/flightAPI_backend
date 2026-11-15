package com.codecool.flightapi.flight;

import com.codecool.flightapi.aircraft.AircraftTypeService;
import com.codecool.flightapi.airline.AirlineService;
import com.codecool.flightapi.airport.AirportService;
import com.codecool.flightapi.common.error.InvalidRequestException;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import com.codecool.flightapi.common.web.Pages;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightService {

    /** Local dates span UTC-12:00 to UTC+14:00, so a local date overlaps these hours around its UTC day. */
    private static final Duration EARLIEST_OFFSET = Duration.ofHours(14);

    private static final Duration LATEST_OFFSET = Duration.ofHours(12);

    private static final Sort ORDER = Sort.by("departureAt", "id");

    /** Shortest airport or city name term a search accepts; IATA codes have exactly this length. */
    static final int MIN_SEARCH_TERM_LENGTH = 3;

    /**
     * Most flights a search may load. Far above any real route on one day, so only overly broad terms
     * (which match most airports) reach it; those are rejected rather than answered with a partial list.
     */
    static final int MAX_SEARCH_ROWS = 500;

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
     * Flights from one airport to another departing on {@code date}, in the departure airport's local time.
     * Each airport is given either as its IATA code ({@code OTP}) or as part of the airport or city name
     * ({@code henri}, {@code London}), ignoring case.
     *
     * @throws InvalidRequestException if a term is shorter than {@value #MIN_SEARCH_TERM_LENGTH} characters, or
     *     the terms match more than {@value #MAX_SEARCH_ROWS} flights
     */
    public List<FlightResponse> search(String from, String to, LocalDate date) {
        requireSearchTerm(from);
        requireSearchTerm(to);
        Instant utcDayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        // one row more than allowed tells a complete result from a cut-off one
        List<Flight> flights = flightRepository.search(
                iataCode(from),
                containsPattern(from),
                iataCode(to),
                containsPattern(to),
                utcDayStart.minus(EARLIEST_OFFSET),
                utcDayStart.plus(Duration.ofDays(1)).plus(LATEST_OFFSET),
                Limit.of(MAX_SEARCH_ROWS + 1));
        if (flights.size() > MAX_SEARCH_ROWS) {
            throw new InvalidRequestException(
                    "The search matches too many flights. Use IATA codes or more specific airport or city names.");
        }
        return flights.stream()
                .filter(flight -> departsOn(flight, date))
                .map(flightMapper::toResponse)
                .toList();
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

    /** Replaces times, price, route, airline and aircraft type (none if the request has no code). */
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
        flight.setAircraftType(
                request.aircraftTypeCode() == null
                        ? null
                        : aircraftTypeService.getByIcaoCode(request.aircraftTypeCode()));
    }

    private static boolean departsOn(Flight flight, LocalDate date) {
        return flight.getDepartureAt()
                .atZone(flight.getDepartureAirport().getTimeZone())
                .toLocalDate()
                .equals(date);
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
