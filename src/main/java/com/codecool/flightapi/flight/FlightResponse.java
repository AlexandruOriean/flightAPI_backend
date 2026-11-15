package com.codecool.flightapi.flight;

import com.codecool.flightapi.aircraft.AircraftTypeResponse;
import com.codecool.flightapi.airline.AirlineResponse;
import com.codecool.flightapi.airport.AirportResponse;
import com.codecool.flightapi.common.money.MoneyDto;
import java.time.OffsetDateTime;

/**
 * A flight with its times in local airport time.
 *
 * @param departure local time and offset at the departure airport
 * @param arrival local time and offset at the arrival airport
 * @param aircraftType {@code null} if the flight has none
 */
public record FlightResponse(
        Long id,
        OffsetDateTime departure,
        OffsetDateTime arrival,
        MoneyDto price,
        AirportResponse departureAirport,
        AirportResponse arrivalAirport,
        AirlineResponse airline,
        AircraftTypeResponse aircraftType) {}
