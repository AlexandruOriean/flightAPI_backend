package com.codecool.flightapi.airport;

import com.codecool.flightapi.city.CityService;
import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import com.codecool.flightapi.common.web.Pages;
import java.util.List;
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
public class AirportService {

    private static final Sort ORDER = Sort.by("id");

    private final AirportRepository airportRepository;
    private final AirportMapper airportMapper;
    private final CityService cityService;

    /** One page of airports, ordered by id. */
    public Page<AirportResponse> findAll(Pageable pageable) {
        return airportRepository.findAll(Pages.ordered(pageable, ORDER)).map(airportMapper::toResponse);
    }

    /**
     * Returns the airport with the given id.
     *
     * @throws ResourceNotFoundException if absent
     */
    public AirportResponse findById(Long id) {
        return airportMapper.toResponse(getById(id));
    }

    /**
     * Returns the airport with the given IATA code (case-insensitive).
     *
     * @throws ResourceNotFoundException if absent
     */
    public AirportResponse findByIataCode(String iataCode) {
        return airportMapper.toResponse(getByIataCode(iataCode));
    }

    /**
     * The airport with the given IATA code (case-insensitive), for other features that reference airports.
     *
     * @throws ResourceNotFoundException if absent
     */
    public Airport getByIataCode(String iataCode) {
        return airportRepository
                .findByIataCode(normalizeIataCode(iataCode))
                .orElseThrow(() -> new ResourceNotFoundException("Airport", "IATA code", iataCode));
    }

    /** Every airport's name, alphabetically. */
    public List<String> findAllNames() {
        return airportRepository.findAllNames();
    }

    /**
     * Creates a new airport in the city named by the request.
     *
     * @throws ResourceNotFoundException if the city does not exist
     * @throws ResourceAlreadyExistsException if an airport with that IATA code exists
     */
    @Transactional
    public AirportResponse create(AirportRequest request) {
        requireIataCodeAvailable(request.iataCode(), null);
        Airport airport = airportMapper.toEntity(request);
        airport.setCity(cityService.getByName(request.cityName()));
        return airportMapper.toResponse(airportRepository.save(airport));
    }

    @Transactional
    public AirportResponse update(Long id, AirportRequest request) {
        Airport airport = getById(id);
        requireIataCodeAvailable(request.iataCode(), id);
        airportMapper.update(request, airport);
        airport.setCity(cityService.getByName(request.cityName()));
        return airportMapper.toResponse(airport);
    }

    @Transactional
    public void delete(Long id) {
        airportRepository.delete(getById(id));
    }

    private Airport getById(Long id) {
        return airportRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.byId("Airport", id));
    }

    private void requireIataCodeAvailable(String iataCode, Long ownId) {
        airportRepository
                .findByIataCode(normalizeIataCode(iataCode))
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException(
                            "An airport with IATA code '" + iataCode + "' already exists.");
                });
    }

    /** IATA codes are stored in upper case; lookups accept any case. */
    private static String normalizeIataCode(String iataCode) {
        return iataCode.strip().toUpperCase(Locale.ROOT);
    }
}
