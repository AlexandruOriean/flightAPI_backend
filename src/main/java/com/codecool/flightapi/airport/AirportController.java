package com.codecool.flightapi.airport;

import com.codecool.flightapi.common.web.ETags;
import com.codecool.flightapi.common.web.Locations;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    /** The {@code ETag} header holds the version to send back in {@code If-Match} when updating. */
    @GetMapping("/{id}")
    public ResponseEntity<AirportResponse> getAirport(@PathVariable Long id) {
        AirportResponse found = airportService.findById(id);
        return ResponseEntity.ok().eTag(ETags.of(found.version())).body(found);
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
        return ResponseEntity.created(Locations.ofCreated(created.id()))
                .eTag(ETags.of(created.version()))
                .body(created);
    }

    /**
     * Replaces the resource. Requires {@code If-Match} with the ETag from the last read: 428 without it, 412 if
     * the resource changed since; the response carries the new ETag.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AirportResponse> updateAirport(
            @PathVariable Long id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody AirportRequest request) {
        AirportResponse updated = airportService.update(id, request, ETags.ifMatch(ifMatch));
        return ResponseEntity.ok().eTag(ETags.of(updated.version())).body(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirport(@PathVariable Long id) {
        airportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
