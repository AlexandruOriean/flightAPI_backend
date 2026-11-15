package com.codecool.flightapi.flight;

import com.codecool.flightapi.aircraft.AircraftTypeMapper;
import com.codecool.flightapi.airline.AirlineMapper;
import com.codecool.flightapi.airport.Airport;
import com.codecool.flightapi.airport.AirportMapper;
import com.codecool.flightapi.common.mapping.MappingConfig;
import com.codecool.flightapi.common.money.MoneyMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Times are stored as instants and shown in the time zone of the airport they refer to. Airports,
 * airline and aircraft type are resolved from their codes by {@link FlightService}, not by the mapper; an
 * update's times are set by the service through {@link Flight#reschedule}.
 */
@Mapper(
        config = MappingConfig.class,
        uses = {AirportMapper.class, AirlineMapper.class, AircraftTypeMapper.class, MoneyMapper.class})
public interface FlightMapper {

    @Mapping(
            target = "departure",
            expression = "java(atAirport(flight.getDepartureAt(), flight.getDepartureAirport()))")
    @Mapping(target = "arrival", expression = "java(atAirport(flight.getArrivalAt(), flight.getArrivalAirport()))")
    FlightResponse toResponse(Flight flight);

    /** Built through the {@link Flight} constructor, which checks the times like {@link Flight#reschedule}. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "departureAt", source = "departure")
    @Mapping(target = "arrivalAt", source = "arrival")
    @Mapping(target = "departureAirport", ignore = true)
    @Mapping(target = "arrivalAirport", ignore = true)
    @Mapping(target = "airline", ignore = true)
    @Mapping(target = "aircraftType", ignore = true)
    Flight toEntity(FlightRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "departureAirport", ignore = true)
    @Mapping(target = "arrivalAirport", ignore = true)
    @Mapping(target = "airline", ignore = true)
    @Mapping(target = "aircraftType", ignore = true)
    void update(FlightRequest request, @MappingTarget Flight flight);

    default Instant toInstant(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant();
    }

    default OffsetDateTime atAirport(Instant instant, Airport airport) {
        return instant.atZone(airport.getTimeZone()).toOffsetDateTime();
    }
}
