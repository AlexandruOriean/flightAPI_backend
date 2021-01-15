package com.codecool.flight_api_project.airport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AirportServiceImpl implements AirportService {

    @Autowired
    private AirportRepository airportRepository;

    @Override
    public Airport saveAirport(Airport airport) {
        return airportRepository.save(airport);
    }

    @Override
    public void deleteAirportById(Long id) {
        airportRepository.deleteById(id);
    }

    @Override
    public Airport updateAirportById(Long id, Airport airportToUpdate) {
        Airport airport = airportRepository.getOne(id);
        airport.setAirportName(airportToUpdate.getAirportName());
        airport.setAirportIataCode(airportToUpdate.getAirportIataCode());
        airport.setCity(airportToUpdate.getCity());
        return airportRepository.save(airport);
    }
}
