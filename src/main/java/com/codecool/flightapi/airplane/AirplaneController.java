package com.codecool.flightapi.airplane;

import com.codecool.flightapi.common.web.Locations;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/airplanes")
@RequiredArgsConstructor
public class AirplaneController {

    private final AirplaneService airplaneService;

    /** One page of airplanes: {@code ?page=0&size=20}. */
    @GetMapping
    public ResponseEntity<PagedModel<AirplaneResponse>> getAirplanes(Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(airplaneService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirplaneResponse> getAirplane(@PathVariable Long id) {
        return ResponseEntity.ok(airplaneService.findById(id));
    }

    @PostMapping
    public ResponseEntity<AirplaneResponse> createAirplane(@Valid @RequestBody AirplaneRequest request) {
        AirplaneResponse created = airplaneService.create(request);
        return ResponseEntity.created(Locations.ofCreated(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AirplaneResponse> updateAirplane(
            @PathVariable Long id, @Valid @RequestBody AirplaneRequest request) {
        return ResponseEntity.ok(airplaneService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirplane(@PathVariable Long id) {
        airplaneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
