package com.codecool.flightapi.aircraft;

public record AircraftTypeResponse(
        Long id, String icaoCode, String manufacturer, String model, int cruiseSpeedKmh, Integer typicalSeats) {}
