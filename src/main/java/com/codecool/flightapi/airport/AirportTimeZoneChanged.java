package com.codecool.flightapi.airport;

/**
 * Published by {@link AirportService} inside the transaction that changed an airport's time zone, so features
 * that store values derived from it can update them in the same transaction.
 */
public record AirportTimeZoneChanged(Long airportId) {}
