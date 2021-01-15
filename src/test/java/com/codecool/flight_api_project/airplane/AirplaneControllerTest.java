package com.codecool.flight_api_project.airplane;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.is;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@WithMockUser(username = "user", roles = "ADMIN")
@SpringBootTest
class AirplaneControllerTest {

    @MockBean
    private AirplaneService airplaneService;

    @Autowired
    private MockMvc mockMvc;


    @Test
    @DisplayName("Test all airplanes found - GET /api/v1/airplanes")
    public void testAllAirplanesFound() throws Exception {
        // Prepare mock airlines
        Airplane mockAirplaneOne = new Airplane(1L, "Boeing", "B373", 160L, 800);
        Airplane mockAirplaneTwo = new Airplane(2L, "Boeing", "B737", 180L, 700);

        List<Airplane> airplanes = new ArrayList<>();
        airplanes.add(mockAirplaneOne);
        airplanes.add(mockAirplaneTwo);

        // Prepare mock service method
        doReturn(airplanes).when(airplaneService).getAllAirplanes();

        // Perform GET request
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/airplanes/"))
                // Validate 200 OK and JSON response type received
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

                // Validate response body
                .andExpect(jsonPath("$[0].model", is("B373")))
                .andExpect(jsonPath("$[1].model", is("B737")));
    }
}