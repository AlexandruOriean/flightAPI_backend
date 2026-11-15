package com.codecool.flightapi.flight;

import com.codecool.flightapi.airline.AirlineResponse;
import com.codecool.flightapi.airport.AirportResponse;
import java.time.LocalDate;
import java.time.LocalTime;

public record FlightResponse(
        Long id,
        LocalDate departureDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        Long price,
        AirportResponse departureAirport,
        AirportResponse arrivalAirport,
        AirlineResponse airline) {}
