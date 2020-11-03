package com.codecool.flight_api_project.flight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/flights")
@CrossOrigin
public class FlightController
{
    @Autowired
    private final FlightRepository flightRepository;

    public FlightController(FlightRepository flightRepository)
    {
        this.flightRepository = flightRepository;
    }

}
