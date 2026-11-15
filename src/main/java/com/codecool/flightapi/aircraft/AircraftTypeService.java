package com.codecool.flightapi.aircraft;

import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import com.codecool.flightapi.common.web.Pages;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AircraftTypeService {

    private static final Sort ORDER = Sort.by("icaoCode");

    private final AircraftTypeRepository aircraftTypeRepository;
    private final AircraftTypeMapper aircraftTypeMapper;

    /** One page of aircraft types, ordered by ICAO code. */
    public Page<AircraftTypeResponse> findAll(Pageable pageable) {
        return aircraftTypeRepository.findAll(Pages.ordered(pageable, ORDER)).map(aircraftTypeMapper::toResponse);
    }

    /**
     * Returns the aircraft type with the given id.
     *
     * @throws ResourceNotFoundException if absent
     */
    public AircraftTypeResponse findById(Long id) {
        return aircraftTypeMapper.toResponse(getById(id));
    }

    /**
     * The aircraft type with the given ICAO code, for other features that reference aircraft types.
     *
     * @throws ResourceNotFoundException if absent
     */
    public AircraftType getByIcaoCode(String icaoCode) {
        return aircraftTypeRepository
                .findByIcaoCode(icaoCode)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft type", "ICAO code", icaoCode));
    }

    /**
     * Creates a new aircraft type.
     *
     * @throws ResourceAlreadyExistsException if an aircraft type with that ICAO code exists
     */
    @Transactional
    public AircraftTypeResponse create(AircraftTypeRequest request) {
        requireIcaoCodeAvailable(request.icaoCode(), null);
        return aircraftTypeMapper.toResponse(aircraftTypeRepository.save(aircraftTypeMapper.toEntity(request)));
    }

    @Transactional
    public AircraftTypeResponse update(Long id, AircraftTypeRequest request) {
        AircraftType aircraftType = getById(id);
        requireIcaoCodeAvailable(request.icaoCode(), id);
        aircraftTypeMapper.update(request, aircraftType);
        return aircraftTypeMapper.toResponse(aircraftType);
    }

    /** Fails with 409 (foreign key) while flights still reference the type. */
    @Transactional
    public void delete(Long id) {
        aircraftTypeRepository.delete(getById(id));
    }

    private AircraftType getById(Long id) {
        return aircraftTypeRepository
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.byId("Aircraft type", id));
    }

    private void requireIcaoCodeAvailable(String icaoCode, Long ownId) {
        aircraftTypeRepository
                .findByIcaoCode(icaoCode)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException(
                            "An aircraft type with ICAO code '" + icaoCode + "' already exists.");
                });
    }
}
