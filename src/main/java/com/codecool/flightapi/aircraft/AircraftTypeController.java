package com.codecool.flightapi.aircraft;

import com.codecool.flightapi.common.web.ETags;
import com.codecool.flightapi.common.web.Locations;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/aircraft-types")
@RequiredArgsConstructor
public class AircraftTypeController {

    private final AircraftTypeService aircraftTypeService;

    /** One page of aircraft types: {@code ?page=0&size=20}. */
    @GetMapping
    public ResponseEntity<PagedModel<AircraftTypeResponse>> getAircraftTypes(Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(aircraftTypeService.findAll(pageable)));
    }

    /** The {@code ETag} header holds the version to send back in {@code If-Match} when updating. */
    @GetMapping("/{id}")
    public ResponseEntity<AircraftTypeResponse> getAircraftType(@PathVariable Long id) {
        AircraftTypeResponse found = aircraftTypeService.findById(id);
        return ResponseEntity.ok().eTag(ETags.of(found.version())).body(found);
    }

    @PostMapping
    public ResponseEntity<AircraftTypeResponse> createAircraftType(@Valid @RequestBody AircraftTypeRequest request) {
        AircraftTypeResponse created = aircraftTypeService.create(request);
        return ResponseEntity.created(Locations.ofCreated(created.id()))
                .eTag(ETags.of(created.version()))
                .body(created);
    }

    /**
     * Replaces the resource. Requires {@code If-Match} with the ETag from the last read: 428 without it, 412 if
     * the resource changed since; the response carries the new ETag.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AircraftTypeResponse> updateAircraftType(
            @PathVariable Long id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody AircraftTypeRequest request) {
        AircraftTypeResponse updated = aircraftTypeService.update(id, request, ETags.ifMatch(ifMatch));
        return ResponseEntity.ok().eTag(ETags.of(updated.version())).body(updated);
    }

    /** 204; 409 while flights still use the type. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAircraftType(@PathVariable Long id) {
        aircraftTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
