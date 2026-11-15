package com.codecool.flightapi.airplane;

public record AirplaneResponse(Long id, String manufacturer, String model, Long numberOfSeats, double speed) {}
