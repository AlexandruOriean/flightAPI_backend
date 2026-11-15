package com.codecool.flightapi.airline;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An airline.
 *
 * @param version the entity's optimistic-lock version; sent as the {@code ETag} header of single-resource
 *     responses, not in the body
 */
public record AirlineResponse(
        Long id, String name, String icaoCode, @JsonIgnore long version) {}
