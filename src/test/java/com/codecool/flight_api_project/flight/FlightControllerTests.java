package com.codecool.flight_api_project.flight;


import com.codecool.flight_api_project.airline.Airline;
import com.codecool.flight_api_project.airport.Airport;
import com.codecool.flight_api_project.city.City;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithMockUser(username = "user", roles = "ADMIN")
public class FlightControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private FlightService flightService;

    @Test
    @DisplayName("Test all flights found - GET /api/v1/flights")
    void testAllFlightsFound() throws Exception {
        City bucuresti = new City(12L, "Bucuresti");
        City londra = new City(21L, "Londra");
        City rome = new City(21L, "Rome");
        Airport otp = new Airport(1L,"Otopeni", "OTP",bucuresti);
        Airport htw = new Airport(2L,"Heathrow", "HTW",londra);
        Airport fmc = new Airport(3L,"Fiumicino", "FMC",rome);
        Airline tarom = new Airline(10000L, "Tarom", "TRO");
        Airline wizz = new Airline(10001L, "Wizz", "WIZZ");
        Flight flight1 = new Flight(1L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(9, 20),LocalTime.of(11, 20),otp,htw,tarom);
        Flight flight2 = new Flight(2L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(12, 20),htw,fmc,wizz);
        Flight flight3 = new Flight(3L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(13, 20),fmc,otp,tarom);
        Flight flight4 = new Flight(4L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(13, 20),htw,otp,tarom);
        Flight flight5 = new Flight(5L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(13, 20),fmc,htw,tarom);

        List<Flight> flightList = new ArrayList<>();
        flightList.add(flight1);
        flightList.add(flight2);
        flightList.add(flight3);
        flightList.add(flight4);
        flightList.add(flight5);

        doReturn(flightList).when(flightService).getAllFlights();

        mockMvc.perform(get("/api/v1/flights"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"));

    }

    @Test
    @DisplayName("Test all flights found by params - GET /api/v1/flights")
    void testAllFlightsFoundByParams() throws Exception {
        City bucuresti = new City(12L, "Bucuresti");
        City londra = new City(21L, "Londra");
        Airport otp = new Airport(1L,"Otopeni", "OTP",bucuresti);
        Airport htw = new Airport(2L,"Heathrow", "HTW",londra);
        Airline tarom = new Airline(10000L, "Tarom", "TRO");
        Airline wizz = new Airline(10001L, "Wizz", "WIZZ");
        Flight flight1 = new Flight(1L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(9, 20),LocalTime.of(11, 20),otp,htw,tarom);
        Flight flight2 = new Flight(2L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(12, 20),otp,htw,wizz);
        Flight flight3 = new Flight(3L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(13, 20),otp,htw,tarom);
        Flight flight4 = new Flight(4L, LocalDate.of(2021, 1,15), 100L, LocalTime.of(10, 20),LocalTime.of(13, 20),otp,htw,tarom);

        List<Flight> flightList = new ArrayList<>();
        flightList.add(flight1);
        flightList.add(flight2);
        flightList.add(flight3);
        flightList.add(flight4);

        doReturn(flightList).when(flightService).getFlightsByParams(otp.getAirportName(),htw.getAirportName(),LocalDate.of(2021, 1,15));

        mockMvc.perform(get("/api/v1/flights"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"));

    }



}
