package com.codecool.flightapi.aircraft;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An aircraft type.
 *
 * @param version the entity's optimistic-lock version; sent as the {@code ETag} header of single-resource
 *     responses, not in the body
 */
public record AircraftTypeResponse(
        Long id,
        String icaoCode,
        String manufacturer,
        String model,
        int cruiseSpeedKmh,
        Integer typicalSeats,
        @JsonIgnore long version) {}
