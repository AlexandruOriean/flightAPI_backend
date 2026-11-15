package com.codecool.flightapi.airplane;

import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AirplaneService {

    private final AirplaneRepository airplaneRepository;
    private final AirplaneMapper airplaneMapper;

    public List<AirplaneResponse> findAll() {
        return airplaneRepository.findAll().stream()
                .map(airplaneMapper::toResponse)
                .toList();
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
