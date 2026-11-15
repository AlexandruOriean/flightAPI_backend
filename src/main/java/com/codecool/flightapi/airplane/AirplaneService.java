package com.codecool.flightapi.airplane;

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
public class AirplaneService {

    private static final Sort ORDER = Sort.by("id");

    private final AirplaneRepository airplaneRepository;
    private final AirplaneMapper airplaneMapper;

    /** One page of airplanes, ordered by id. */
    public Page<AirplaneResponse> findAll(Pageable pageable) {
        return airplaneRepository.findAll(Pages.ordered(pageable, ORDER)).map(airplaneMapper::toResponse);
    }

    /**
     * Returns the airplane with the given id.
     *
     * @throws ResourceNotFoundException if absent
     */
    public AirplaneResponse findById(Long id) {
        return airplaneMapper.toResponse(getById(id));
    }

    @Transactional
    public AirplaneResponse create(AirplaneRequest request) {
        return airplaneMapper.toResponse(airplaneRepository.save(airplaneMapper.toEntity(request)));
    }

    @Transactional
    public AirplaneResponse update(Long id, AirplaneRequest request) {
        Airplane airplane = getById(id);
        airplaneMapper.update(request, airplane);
        return airplaneMapper.toResponse(airplane);
    }

    @Transactional
    public void delete(Long id) {
        airplaneRepository.delete(getById(id));
    }

    private Airplane getById(Long id) {
        return airplaneRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.byId("Airplane", id));
    }
}
