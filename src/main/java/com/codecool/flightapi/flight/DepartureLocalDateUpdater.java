package com.codecool.flightapi.flight;

import com.codecool.flightapi.airport.AirportTimeZoneChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Keeps {@link Flight#getDepartureLocalDate()} right when a departure airport moves to another time zone. */
@Component
@RequiredArgsConstructor
class DepartureLocalDateUpdater {

    private final FlightRepository flightRepository;

    /** Runs synchronously in the airport update's transaction: the new zone and the new dates commit together. */
    @EventListener
    void onTimeZoneChanged(AirportTimeZoneChanged event) {
        flightRepository.findByDepartureAirportId(event.airportId()).forEach(Flight::refreshDepartureLocalDate);
    }
}
