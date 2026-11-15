package com.codecool.flightapi.airline;

import com.codecool.flightapi.common.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** {@code Airline.iso} holds the ICAO designator; the API calls it {@code icaoCode}. */
@Mapper(config = MappingConfig.class)
public interface AirlineMapper {

    @Mapping(target = "icaoCode", source = "iso")
    AirlineResponse toResponse(Airline airline);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "iso", source = "icaoCode")
    Airline toEntity(AirlineRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "iso", source = "icaoCode")
    void update(AirlineRequest request, @MappingTarget Airline airline);
}
