package com.codecool.flightapi.city;

import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CityService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;

    public List<CityResponse> findAll() {
        return cityRepository.findAll().stream().map(cityMapper::toResponse).toList();
    }

    /**
     * Returns the city with the given id.
     *
     * @throws ResourceNotFoundException if absent
     */
    public CityResponse findById(Long id) {
        return cityMapper.toResponse(getById(id));
    }

    /**
     * The city with the given name (case-insensitive), for other features that reference cities.
     *
     * @throws ResourceNotFoundException if absent
     */
    public City getByName(String cityName) {
        return cityRepository
                .findByCityNameIgnoreCase(cityName)
                .orElseThrow(() -> new ResourceNotFoundException("City", "name", cityName));
    }

    /**
     * Creates a new city.
     *
     * @throws ResourceAlreadyExistsException if a city with that name exists
     */
    @Transactional
    public CityResponse create(CityRequest request) {
        requireNameAvailable(request.cityName(), null);
        return cityMapper.toResponse(cityRepository.save(cityMapper.toEntity(request)));
    }

    @Transactional
    public CityResponse update(Long id, CityRequest request) {
        City city = getById(id);
        requireNameAvailable(request.cityName(), id);
        cityMapper.update(request, city);
        return cityMapper.toResponse(city);
    }

    @Transactional
    public void delete(Long id) {
        cityRepository.delete(getById(id));
    }

    private City getById(Long id) {
        return cityRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.byId("City", id));
    }

    private void requireNameAvailable(String cityName, Long ownId) {
        cityRepository
                .findByCityNameIgnoreCase(cityName)
                .filter(existing -> !existing.getCityId().equals(ownId))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("City '" + cityName + "' already exists.");
                });
    }
}
