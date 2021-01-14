package com.codecool.flight_api_project.flight;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface FlightService {

    Flight saveFlight(Flight flight);

    void deleteFlightById(Long id);

    Flight updateFlightById(Long id, Flight flight);

    List<Flight> getFlightsByParams(String airportName, String airportName1, LocalDate date);

    List<Flight> getAllFlights();
}
