package com.codecool.flightapi.airport;

import com.codecool.flightapi.city.CityResponse;

public record AirportResponse(Long id, String iataCode, String name, CityResponse city, String timeZone) {}
