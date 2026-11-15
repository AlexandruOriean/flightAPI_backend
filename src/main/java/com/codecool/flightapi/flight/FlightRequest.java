package com.codecool.flightapi.flight;

import com.codecool.flightapi.common.money.MoneyDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;

/**
 * Body of {@code POST/PUT /api/v1/flights}. Airports and airline are referenced by their codes and must
 * already exist.
 *
 * @param departureAirportCode IATA code, e.g. {@code OTP}
 * @param arrivalAirportCode IATA code, e.g. {@code LHR}
 * @param airlineCode ICAO airline designator, e.g. {@code ROT}
 * @param departure with offset, e.g. {@code 2026-12-01T09:30:00+02:00}
 * @param arrival with offset, e.g. {@code 2026-12-01T11:05:00Z}
 */
public record FlightRequest(
        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be 3 upper-case letters") String departureAirportCode,

        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be 3 upper-case letters") String arrivalAirportCode,

        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be 3 upper-case letters") String airlineCode,

        @NotNull OffsetDateTime departure,
        @NotNull OffsetDateTime arrival,
        @NotNull @Valid MoneyDto price) {

    @AssertTrue(message = "departure and arrival airport must differ") public boolean isDifferentAirports() {
        return departureAirportCode == null || !departureAirportCode.equals(arrivalAirportCode);
    }

    @AssertTrue(message = "arrival must be after departure") public boolean isArrivalAfterDeparture() {
        return departure == null || arrival == null || arrival.isAfter(departure);
    }
}
