package com.codecool.flightapi.city;

import com.codecool.flightapi.common.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MappingConfig.class)
public interface CityMapper {

    @Mapping(target = "id", source = "cityId")
    CityResponse toResponse(City city);

    @Mapping(target = "cityId", ignore = true)
    City toEntity(CityRequest request);

    @Mapping(target = "cityId", ignore = true)
    void update(CityRequest request, @MappingTarget City city);
}
