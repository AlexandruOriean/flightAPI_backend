package com.codecool.flightapi.airport;

import com.codecool.flightapi.city.CityResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An airport with its city.
 *
 * @param version the entity's optimistic-lock version; sent as the {@code ETag} header of single-resource
 *     responses, not in the body
 */
public record AirportResponse(
        Long id,
        String iataCode,
        String name,
        CityResponse city,
        String timeZone,
        @JsonIgnore long version) {}
