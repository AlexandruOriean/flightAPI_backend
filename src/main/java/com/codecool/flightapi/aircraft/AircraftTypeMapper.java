package com.codecool.flightapi.aircraft;

import com.codecool.flightapi.common.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MappingConfig.class)
public interface AircraftTypeMapper {

    AircraftTypeResponse toResponse(AircraftType aircraftType);

    @Mapping(target = "id", ignore = true)
    AircraftType toEntity(AircraftTypeRequest request);

    @Mapping(target = "id", ignore = true)
    void update(AircraftTypeRequest request, @MappingTarget AircraftType aircraftType);
}
