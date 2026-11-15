package com.codecool.flightapi.airline;

import com.codecool.flightapi.common.web.Locations;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    @GetMapping
    public ResponseEntity<List<AirlineResponse>> getAirlines() {
        return ResponseEntity.ok(airlineService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirlineResponse> getAirline(@PathVariable Long id) {
        return ResponseEntity.ok(airlineService.findById(id));
    }

    @PostMapping
    public ResponseEntity<AirlineResponse> createAirline(@Valid @RequestBody AirlineRequest request) {
        AirlineResponse created = airlineService.create(request);
        return ResponseEntity.created(Locations.ofCreated(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AirlineResponse> updateAirline(
            @PathVariable Long id, @Valid @RequestBody AirlineRequest request) {
        return ResponseEntity.ok(airlineService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirline(@PathVariable Long id) {
        airlineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
