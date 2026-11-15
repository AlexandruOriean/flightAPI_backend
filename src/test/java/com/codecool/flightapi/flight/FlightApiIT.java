package com.codecool.flightapi.flight;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The public JSON contract of flights and the admin write endpoints that reference other records by
 * their codes, against the full application and the Flyway seed data. Each test is rolled back.
 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:flight-api-contract-it;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            // Test-only key (64 random bytes, Base64). Never use outside tests.
            "app.jwt.secret=dGVzdC1vbmx5LWtleS1kby1ub3QtdXNlLWluLXByb2R1Y3Rpb24tMDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3A="
        })
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class FlightApiIT {

    private static final String NEW_FLIGHT = """
            {"departureAirportCode":"OTP","arrivalAirportCode":"CDG","airlineCode":"AFR",
             "departureDate":"2026-12-01","departureTime":"08:15:00","arrivalTime":"10:45:00","price":120}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("flight JSON: id, schedule, price, nested airports with city, airline with ICAO code")
    void flightResponseShape() throws Exception {
        mockMvc.perform(get("/api/v1/flights/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.flight_id").doesNotExist())
                .andExpect(jsonPath("$.departureDate").value("2020-11-01"))
                .andExpect(jsonPath("$.departureTime").value("09:30:00"))
                .andExpect(jsonPath("$.price").value(35))
                .andExpect(jsonPath("$.departureAirport.iataCode").value("LHR"))
                .andExpect(jsonPath("$.departureAirport.name").value("London Heathrow Airport"))
                .andExpect(jsonPath("$.departureAirport.city.cityName").value("London"))
                .andExpect(jsonPath("$.arrivalAirport.city.cityName").value("Bucuresti"))
                .andExpect(jsonPath("$.airline.name").value("Tarom"))
                .andExpect(jsonPath("$.airline.icaoCode").value("ROT"));
    }

    @Test
    @DisplayName("POST flight with airport and airline codes -> 201 with the resolved records")
    void createFlightByCodes() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_FLIGHT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.departureAirport.name").value("Henri Coanda International Airport"))
                .andExpect(jsonPath("$.arrivalAirport.iataCode").value("CDG"))
                .andExpect(jsonPath("$.airline.name").value("Air France"));
    }

    @Test
    @DisplayName("PUT flight replaces route, airline, schedule and price")
    void updateFlight() throws Exception {
        mockMvc.perform(put("/api/v1/flights/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_FLIGHT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.departureAirport.iataCode").value("OTP"))
                .andExpect(jsonPath("$.airline.icaoCode").value("AFR"))
                .andExpect(jsonPath("$.price").value(120));
    }

    @Test
    @DisplayName("POST flight with an unknown airport code -> 404 problem+json")
    void unknownAirportCodeIsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_FLIGHT.replace("\"CDG\"", "\"XXX\"")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Airport with IATA code 'XXX' was not found"));
    }

    @Test
    @DisplayName("POST flight with invalid codes or the same airport twice -> 400 listing the fields")
    void invalidFlightIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_FLIGHT.replace("\"CDG\"", "\"OTP\"").replace("\"AFR\"", "\"afr\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("airlineCode")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("differentAirports")));
    }

    @Test
    @DisplayName("POST airport with an IATA code that already exists -> 409 problem+json")
    void duplicateAirportIsConflict() throws Exception {
        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iataCode\":\"OTP\",\"name\":\"Duplicate\",\"cityName\":\"bucuresti\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("POST airport resolves its city by name, ignoring case")
    void createAirportByCityName() throws Exception {
        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iataCode\":\"BBU\",\"name\":\"Aurel Vlaicu\",\"cityName\":\"bucuresti\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.iataCode").value("BBU"))
                .andExpect(jsonPath("$.city.cityName").value("Bucuresti"));
    }
}
