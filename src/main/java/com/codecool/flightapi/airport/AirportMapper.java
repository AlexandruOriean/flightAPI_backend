package com.codecool.flightapi.airport;

import com.codecool.flightapi.city.CityMapper;
import com.codecool.flightapi.common.mapping.MappingConfig;
import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** The city is resolved from {@code cityName} by {@link AirportService}, not by the mapper. */
@Mapper(config = MappingConfig.class, uses = CityMapper.class)
public interface AirportMapper {

    @Mapping(target = "iataCode", source = "airportIataCode")
    @Mapping(target = "name", source = "airportName")
    AirportResponse toResponse(Airport airport);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "airportIataCode", source = "iataCode")
    @Mapping(target = "airportName", source = "name")
    Airport toEntity(AirportRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "airportIataCode", source = "iataCode")
    @Mapping(target = "airportName", source = "name")
    void update(AirportRequest request, @MappingTarget Airport airport);

    default String zoneId(ZoneId zone) {
        return zone == null ? null : zone.getId();
    }

    default ZoneId zoneId(String id) {
        return id == null ? null : ZoneId.of(id);
    }
}
