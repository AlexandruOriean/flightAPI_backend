package com.codecool.flightapi.flight;

import com.codecool.flightapi.common.web.Locations;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    // query parameters of the flight search
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String DATE = "date";

    /** All search parameters; {@code SecurityConfig} keeps requests with any of them public. */
    public static final List<String> SEARCH_PARAMETERS = List.of(FROM, TO, DATE);

    /** Longer than any airport or city name; keeps the LIKE patterns of the search short. */
    private static final int SEARCH_TERM_MAX_LENGTH = 100;

    private final FlightService flightService;

    /** One page of flights: {@code ?page=0&size=20}. Requests with any search parameter go to the search. */
    @GetMapping(params = {"!" + FROM, "!" + TO, "!" + DATE})
    public ResponseEntity<PagedModel<FlightResponse>> getFlights(Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(flightService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightResponse> getFlight(@PathVariable Long id) {
        return ResponseEntity.ok(flightService.findById(id));
    }

    /**
     * Flights departing on one day: {@code ?from=OTP&to=LHR&date=2026-01-15}. All three parameters are
     * required (400 otherwise); see {@link FlightService#search} for how airports are matched.
     */
    @GetMapping
    public ResponseEntity<List<FlightResponse>> searchFlights(
            @RequestParam(FROM) @NotBlank @Size(max = SEARCH_TERM_MAX_LENGTH) String from,
            @RequestParam(TO) @NotBlank @Size(max = SEARCH_TERM_MAX_LENGTH) String to,
            @RequestParam(DATE) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(flightService.search(from, to, date));
    }

    @PostMapping
    public ResponseEntity<FlightResponse> createFlight(@Valid @RequestBody FlightRequest request) {
        FlightResponse created = flightService.create(request);
        return ResponseEntity.created(Locations.ofCreated(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlightResponse> updateFlight(
            @PathVariable Long id, @Valid @RequestBody FlightRequest request) {
        return ResponseEntity.ok(flightService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id) {
        flightService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
