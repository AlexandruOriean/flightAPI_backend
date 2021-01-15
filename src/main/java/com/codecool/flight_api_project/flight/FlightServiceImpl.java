package com.codecool.flight_api_project.flight;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


@Service
public class FlightServiceImpl implements FlightService{

    @Autowired
    private FlightRepository flightRepository;

    @Override
    public Flight saveFlight(Flight flight) {
        return flightRepository.save(flight);
    }

    @Override
    public void deleteFlightById(Long id) {
        flightRepository.deleteById(id);
    }

    @Override
    public Flight updateFlightById(Long id, Flight flightToUpdate) {
        Flight flight = flightRepository.findById(id).get();
        flight.setDepartureTime(flightToUpdate.getDepartureTime());
        flight.setArrivalTime(flightToUpdate.getArrivalTime());
        flight.setPrice(flightToUpdate.getPrice());
        flight.setDepartureDate(flightToUpdate.getDepartureDate());
        return flightRepository.save(flight);
    }

    @Override
    public List<Flight> getFlightsByParams(String airportName, String airportName1, LocalDate date) {
        return flightRepository.getFlightByParams(airportName,airportName1,date);
    }

    @Override
    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }
}
