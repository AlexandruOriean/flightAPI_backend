package com.codecool.flightapi.airline;

import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirlineMapper airlineMapper;

    public List<AirlineResponse> findAll() {
        return airlineRepository.findAll().stream()
                .map(airlineMapper::toResponse)
                .toList();
    }

    /**
     * Returns the airline with the given id.
     *
     * @throws ResourceNotFoundException if absent
     */
    public AirlineResponse findById(Long id) {
        return airlineMapper.toResponse(getById(id));
    }

    /**
     * The airline with the given ICAO code, for other features that reference airlines.
     *
     * @throws ResourceNotFoundException if absent
     */
    public Airline getByIcaoCode(String icaoCode) {
        return airlineRepository
                .findByIcaoCode(icaoCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", "ICAO code", icaoCode));
    }

    /**
     * Creates a new airline.
     *
     * @throws ResourceAlreadyExistsException if an airline with that ICAO code exists
     */
    @Transactional
    public AirlineResponse create(AirlineRequest request) {
        requireIcaoCodeAvailable(request.icaoCode(), null);
        return airlineMapper.toResponse(airlineRepository.save(airlineMapper.toEntity(request)));
    }

    @Transactional
    public AirlineResponse update(Long id, AirlineRequest request) {
        Airline airline = getById(id);
        requireIcaoCodeAvailable(request.icaoCode(), id);
        airlineMapper.update(request, airline);
        return airlineMapper.toResponse(airline);
    }

    @Transactional
    public void delete(Long id) {
        airlineRepository.delete(getById(id));
    }

    private Airline getById(Long id) {
        return airlineRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.byId("Airline", id));
    }

    private void requireIcaoCodeAvailable(String icaoCode, Long ownId) {
        airlineRepository
                .findByIcaoCode(icaoCode)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException(
                            "An airline with ICAO code '" + icaoCode + "' already exists.");
                });
    }
}
