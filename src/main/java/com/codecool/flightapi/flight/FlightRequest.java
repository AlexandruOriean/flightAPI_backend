package com.codecool.flightapi.flight;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Body of {@code POST/PUT /api/v1/flights}. Airports and airline are referenced by their codes and must
 * already exist.
 *
 * @param departureAirportCode IATA code, e.g. {@code OTP}
 * @param arrivalAirportCode IATA code, e.g. {@code LHR}
 * @param airlineCode ICAO airline designator, e.g. {@code ROT}
 */
public record FlightRequest(
        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be 3 upper-case letters") String departureAirportCode,

        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be 3 upper-case letters") String arrivalAirportCode,

        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be 3 upper-case letters") String airlineCode,

        @NotNull LocalDate departureDate,
        @NotNull LocalTime departureTime,
        @NotNull LocalTime arrivalTime,
        @NotNull @PositiveOrZero Long price) {

    @AssertTrue(message = "departure and arrival airport must differ") public boolean isDifferentAirports() {
        return departureAirportCode == null || !departureAirportCode.equals(arrivalAirportCode);
    }
}
