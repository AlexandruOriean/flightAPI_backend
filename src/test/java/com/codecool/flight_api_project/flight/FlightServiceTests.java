package com.codecool.flight_api_project.flight;

import com.codecool.flight_api_project.airline.Airline;
import com.codecool.flight_api_project.airport.Airport;
import com.codecool.flight_api_project.city.City;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class FlightServiceTests {
    @Autowired
    FlightService flightService;
    @MockBean
    FlightRepository flightRepository;

    @Test
    @DisplayName("Test all flights found in database - Service")
    void testAllFlightsFoundDB(){

        City bucuresti = new City(12L, "Bucuresti");
        City londra = new City(21L, "Londra");
        City rome = new City(21L, "Rome");
        Airport otp = new Airport(1L,"Otopeni", "OTP",bucuresti);
        Airport htw = new Airport(2L,"Heathrow", "HTW",londra);
        Airport fmc = new Airport(3L,"Fiumicino", "FMC",rome);
        Airline tarom = new Airline(10000L, "Tarom", "TRO");
        Airline wizz = new Airline(10001L, "Wizz", "WIZZ");
        when(flightRepository.findAll()).thenReturn(
                (Stream.of(new Flight(1L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(9, 20),LocalTime.of(11, 20),otp,htw,tarom),
                        new Flight(2L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(12, 20),htw,fmc,wizz),
                        new Flight(3L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(13, 20),fmc,otp,tarom),
                        new Flight(4L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(13, 20),htw,otp,tarom),
                        new Flight(5L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(13, 20),fmc,htw,tarom)
                )).collect(Collectors.toList()));

        assertEquals(5, flightService.getAllFlights().size());

    }

    @Test
    @DisplayName("Test all flights found by Params - Service")
    void getFlightsByParamsTest(){
        City cityFrom = new City(12L, "Bucuresti");
        City cityTo = new City(21L, "Londra");
        Airport airportFrom = new Airport(1L,"Otopeni", "OTP",cityFrom);
        Airport airportTo = new Airport(2L,"Heathrow", "HTW",cityTo);
        Airline airline = new Airline(10000L, "Tarom", "TRO");

        when(flightRepository.getFlightByParams(airportFrom.getAirportName(),
                airportTo.getAirportName(), LocalDate.of(2021, 1,15))).thenReturn(
                (Stream.of(new Flight(1L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(9, 20),LocalTime.of(11, 20),airportFrom,airportTo,airline),
                        new Flight(1L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(12, 20),airportFrom,airportTo,airline),
                        new Flight(1L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(13, 20),airportFrom,airportTo,airline)
                )).collect(Collectors.toList()));
        assertEquals(3,flightService.getFlightsByParams(airportFrom.getAirportName(),airportTo.getAirportName(), LocalDate.of(2021, 1,15)).size());
    }


}
