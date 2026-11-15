package com.codecool.flightapi.airplane;

import com.codecool.flightapi.common.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MappingConfig.class)
public interface AirplaneMapper {

    AirplaneResponse toResponse(Airplane airplane);

    @Mapping(target = "id", ignore = true)
    Airplane toEntity(AirplaneRequest request);

    @Mapping(target = "id", ignore = true)
    void update(AirplaneRequest request, @MappingTarget Airplane airplane);
}
