package com.codecool.flight_api_project.airline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/v1/airlines")
@CrossOrigin
public class AirlineController {

    @Autowired
    private AirlineService airlineService;


    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<Airline> getAirlines() {
        return airlineService.getAllAirlines();
    }

    @PostMapping()
    public ResponseEntity<Airline> addAirline(@RequestBody final Airline airline) {
        Airline savedAirline = airlineService.saveAirline(airline);
        return new ResponseEntity<>(savedAirline, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAirlineById(
            @PathVariable("id") final Long id) {
        airlineService.deleteAirlineById(id);
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Airline> updateAirlineById(
            @PathVariable("id") final Long id,
            @RequestBody final Airline airlineToUpdate) {
        Airline updatedAirline
                = airlineService.updateAirlineById(id, airlineToUpdate);
        return new ResponseEntity<>(updatedAirline, HttpStatus.OK);
    }
}