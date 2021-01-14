package com.codecool.flight_api_project.airportControllerTest;

import com.codecool.flight_api_project.airport.Airport;
import com.codecool.flight_api_project.airport.AirportService;
import com.codecool.flight_api_project.city.City;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username="user",roles = "ADMIN")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AirportControllerTest {

    @MockBean
    private AirportService airportService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Add a new airplane - POST /api/v1/airports")
    public void saveAirportTest() throws Exception {
        // Prepare mock product
        Airport airport1 = new Airport(1112L,"AAH","Aachen-Merzbrück Airport",new City(2L,"Aachen"));

        // Prepare mock service method
        doReturn(airport1).when(airportService).saveAirport(ArgumentMatchers.any());

        // Perform POST request
        mockMvc.perform(post("/api/v1/airports")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(new ObjectMapper().writeValueAsString(airport1)))

                // Validate 201 CREATED and JSON response type received
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))

                // Validate response headers
                .andExpect(header().string(HttpHeaders.VARY, "Origin"))
                .andExpect(header().string(HttpHeaders.PRAGMA,"no-cache"))

                // Validate response body
                .andExpect(jsonPath("$.id", is(1112L)))
                .andExpect(jsonPath("$.airportIataCode", is("AAH")))
                .andExpect(jsonPath("$.airportName", is("Aachen-Merzbrück Airport")))
                .andExpect(jsonPath("$.city", is(new City(2L,"Aachen"))));
    }
}
