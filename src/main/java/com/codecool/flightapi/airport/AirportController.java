package com.codecool.flightapi.airport;

import com.codecool.flightapi.common.web.Locations;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    /** One page of airports: {@code ?page=0&size=20}. */
    @GetMapping
    public ResponseEntity<PagedModel<AirportResponse>> getAirports(Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(airportService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirportResponse> getAirport(@PathVariable Long id) {
        return ResponseEntity.ok(airportService.findById(id));
    }

    @GetMapping("/iata/{iataCode}")
    public ResponseEntity<AirportResponse> getAirportByIataCode(@PathVariable String iataCode) {
        return ResponseEntity.ok(airportService.findByIataCode(iataCode));
    }

    @GetMapping("/names")
    public ResponseEntity<List<String>> getAirportNames() {
        return ResponseEntity.ok(airportService.findAllNames());
    }

    @PostMapping
    public ResponseEntity<AirportResponse> createAirport(@Valid @RequestBody AirportRequest request) {
        AirportResponse created = airportService.create(request);
        return ResponseEntity.created(Locations.ofCreated(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AirportResponse> updateAirport(
            @PathVariable Long id, @Valid @RequestBody AirportRequest request) {
        return ResponseEntity.ok(airportService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirport(@PathVariable Long id) {
        airportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
