package com.codecool.flightapi.flight;

import com.codecool.flightapi.airline.AirlineMapper;
import com.codecool.flightapi.airport.AirportMapper;
import com.codecool.flightapi.common.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** Airports and airline are resolved from their codes by {@link FlightService}, not by the mapper. */
@Mapper(
        config = MappingConfig.class,
        uses = {AirportMapper.class, AirlineMapper.class})
public interface FlightMapper {

    @Mapping(target = "id", source = "flight_id")
    FlightResponse toResponse(Flight flight);

    @Mapping(target = "flight_id", ignore = true)
    @Mapping(target = "departureAirport", ignore = true)
    @Mapping(target = "arrivalAirport", ignore = true)
    @Mapping(target = "airline", ignore = true)
    Flight toEntity(FlightRequest request);

    @Mapping(target = "flight_id", ignore = true)
    @Mapping(target = "departureAirport", ignore = true)
    @Mapping(target = "arrivalAirport", ignore = true)
    @Mapping(target = "airline", ignore = true)
    void update(FlightRequest request, @MappingTarget Flight flight);
}
