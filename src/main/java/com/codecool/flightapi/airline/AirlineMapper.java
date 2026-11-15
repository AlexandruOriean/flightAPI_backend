package com.codecool.flightapi.airline;

import com.codecool.flightapi.common.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MappingConfig.class)
public interface AirlineMapper {

    AirlineResponse toResponse(Airline airline);

    @Mapping(target = "id", ignore = true)
    Airline toEntity(AirlineRequest request);

    @Mapping(target = "id", ignore = true)
    void update(AirlineRequest request, @MappingTarget Airline airline);
}
