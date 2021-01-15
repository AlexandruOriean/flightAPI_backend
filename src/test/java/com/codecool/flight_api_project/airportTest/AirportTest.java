package com.codecool.flight_api_project.airportTest;

import com.codecool.flight_api_project.airport.Airport;
import com.codecool.flight_api_project.airport.AirportRepository;
import com.codecool.flight_api_project.airport.AirportService;
import com.codecool.flight_api_project.city.City;
import com.codecool.flight_api_project.city.CityRepository;
import com.codecool.flight_api_project.city.CityService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AirportTest {

    @Autowired
    private AirportService airportService;

    @MockBean
    private AirportRepository airportRepository;


    @Test
    public void updateAirportByIdTest(){
        Airport airport = new Airport(1112L,"AAH","Aachen-Merzbrück Airport",new City(2L,"Aachen"));
        Airport updatedAirport = new Airport(1112L,"UTK","Amsterdam",new City(2L,"Aachen"));

        doReturn(airport).when(airportRepository).getOne(1112L);
        doReturn(updatedAirport).when(airportRepository).save(airport);

        updatedAirport = airportService.updateAirportById(airport.getId(),airport);
        Assertions.assertEquals("Amsterdam",updatedAirport.getAirportName());
    }

    @Test
    public void saveAirportTest(){
        Airport airport =  new Airport(1112L,"AAH","Aachen-Merzbrück Airport",new City(2L,"Aachen"));
        when(airportRepository.save(airport)).thenReturn(airport);
        assertEquals(airport,airportService.saveAirport(airport));

    }

    @Test
    public void deleteAirportByIdTest(){
        Long airportId = 65L;
        airportService.deleteAirportById(airportId);
        verify(airportRepository,times(1)).deleteById(airportId);

    }



}