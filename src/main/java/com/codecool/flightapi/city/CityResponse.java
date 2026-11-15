package com.codecool.flightapi.city;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A city.
 *
 * @param version the entity's optimistic-lock version; sent as the {@code ETag} header of single-resource
 *     responses, not in the body
 */
public record CityResponse(Long id, String name, @JsonIgnore long version) {}
