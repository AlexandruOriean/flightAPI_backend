package com.codecool.flight_api_project.airline;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "user", roles = "ADMIN")
public class AirlineControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AirlineService airlineService;

    @Test
    @DisplayName("Test all airlines found - GET /api/v1/airlines")
    public void testAllAirlinesFound() throws Exception {
        // Prepare mock airlines
        Airline tarom = new Airline(10000L, "Tarom", "TRO");
        Airline wizz = new Airline(10001L, "Wizz", "WIZZ");
        Airline blueAir = new Airline(10002L, "BlueAir", "BLA");

        List<Airline> airlines = new ArrayList<>();
        airlines.add(tarom);
        airlines.add(wizz);
        airlines.add(blueAir);

//        Prepare mock service method
        doReturn(airlines).when(airlineService).getAllAirlines();

//        Perform GET request
        mockMvc.perform(get("/api/v1/airlines"))
//                Validate 200 OK and JSON response type received
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

//                Validate response body
                .andExpect(jsonPath("$[0].name", is("Tarom")))
                .andExpect(jsonPath("$[1].name", is("Wizz")))
                .andExpect(jsonPath("$[2].name", is("BlueAir")));

    }

    @Test
    @DisplayName("Add a new Airline - POST /api/v1/airlines")
    void testAddNewAirline() throws Exception {
//        Prepare mock airline
        Airline tarom = new Airline((long) 10000, "Tarom", "TRO");

//        Prepare mock service method
        doReturn(tarom).when(airlineService).saveAirline(ArgumentMatchers.any());

//        Perform POST request
        mockMvc.perform(post("/api/v1/airlines")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(new ObjectMapper().writeValueAsString(tarom)))

                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))

                .andExpect(header().string(HttpHeaders.VARY, "Origin"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"))

                .andExpect(jsonPath("$.id", is(10000)))
                .andExpect(jsonPath("$.name", is("Tarom")))
                .andExpect(jsonPath("$.iso", is("TRO")));

    }

    @Test
    @DisplayName("Update an existing airline with succes - PUT /api/v1/airlines/10000")
    public void testUpdatingAirlineWithSucces() throws Exception{
        Airline mockAirline = new Airline((long) 10000, "Tarom", "TRO");
        Airline airlineToUpdate = new Airline((long) 10000, "Wizz", "WIZZ");

        doReturn(mockAirline).when(airlineService).findById((long) 10000);
        doReturn(mockAirline).when(airlineService).updateAirlineById(ArgumentMatchers.any(), ArgumentMatchers.any());

        mockMvc.perform(put("/api/v1/airlines/{id}",1)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header(HttpHeaders.IF_MATCH, 10000)
        .content(new ObjectMapper().writeValueAsString(airlineToUpdate)))

                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))

                .andExpect(header().string(HttpHeaders.VARY, "Origin"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"))

                .andExpect(jsonPath("$.id", is(10000)))
                .andExpect(jsonPath("$.name", is("Tarom")))
                .andExpect(jsonPath("$.iso", is("TRO")));

    }

    @Test
    @DisplayName("Delete an airline - DELETE /api/v1/airlines/10000")
    public void testDeleteAirline() throws Exception {
        Airline existingAirline = new Airline((long) 10000, "Tarom", "TRO");

        doReturn(existingAirline).when(airlineService).findById((long) 10000);

        mockMvc.perform(delete( "/api/v1/airlines/{id}", 1))
                .andExpect(status().isOk());
    }
}