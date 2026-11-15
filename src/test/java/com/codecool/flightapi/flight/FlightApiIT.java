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

    /** OTP 08:15 local (06:15Z) -> CDG 09:45 local (08:45Z). */
    private static final String NEW_FLIGHT = """
            {"departureAirportCode":"OTP","arrivalAirportCode":"CDG","airlineCode":"AFR",
             "departure":"2026-12-01T08:15:00+02:00","arrival":"2026-12-01T09:45:00+01:00",
             "price":{"amount":120.50,"currency":"EUR"}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("flight JSON: id, local times with offset, price with currency, airports with city and zone")
    void flightResponseShape() throws Exception {
        // seed flight 1 (LHR -> OTP), converted by V7: 09:30 London time, 3h15 block time, GBP
        mockMvc.perform(get("/api/v1/flights/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.departure").value("2020-11-01T09:30:00Z"))
                .andExpect(jsonPath("$.arrival").value("2020-11-01T14:45:00+02:00"))
                .andExpect(jsonPath("$.price.amount").value(35.0))
                .andExpect(jsonPath("$.price.currency").value("GBP"))
                // validation helpers of the DTO are not part of the JSON
                .andExpect(jsonPath("$.price.knownCurrency").doesNotExist())
                .andExpect(jsonPath("$.departureAirport.iataCode").value("LHR"))
                .andExpect(jsonPath("$.departureAirport.timeZone").value("Europe/London"))
                .andExpect(jsonPath("$.departureAirport.city.cityName").value("London"))
                .andExpect(jsonPath("$.arrivalAirport.timeZone").value("Europe/Bucharest"))
                .andExpect(jsonPath("$.airline.icaoCode").value("ROT"))
                .andExpect(jsonPath("$.departureDate").doesNotExist())
                .andExpect(jsonPath("$.flight_id").doesNotExist());
    }

    @Test
    @DisplayName("V7 gave existing flights the currency of their departure airport's country")
    void migratedPricesUseTheDepartureCountryCurrency() throws Exception {
        mockMvc.perform(get("/api/v1/flights/2")) // OTP -> LHR
                .andExpect(jsonPath("$.departure").value("2020-11-01T09:30:00+02:00"))
                .andExpect(jsonPath("$.price.currency").value("RON"));
        mockMvc.perform(get("/api/v1/flights/3")) // CDG -> OTP
                .andExpect(jsonPath("$.departure").value("2020-11-01T09:30:00+01:00"))
                .andExpect(jsonPath("$.price.currency").value("EUR"));
    }

    @Test
    @DisplayName("search matches the local date at the departure airport, not the UTC date")
    void searchUsesLocalDepartureDate() throws Exception {
        // 00:30 in Bucharest on 2 December is still 1 December in UTC
        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_FLIGHT
                                .replace("2026-12-01T08:15:00+02:00", "2026-12-02T00:30:00+02:00")
                                .replace("2026-12-01T09:45:00+01:00", "2026-12-02T02:00:00+01:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/flights?from=OTP&to=CDG&date=2026-12-02"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].departure").value("2026-12-02T00:30:00+02:00"));
        mockMvc.perform(get("/api/v1/flights?from=OTP&to=CDG&date=2026-12-01"))
                .andExpect(jsonPath("$.length()").value(0));
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
                .andExpect(jsonPath("$.airline.name").value("Air France"))
                .andExpect(jsonPath("$.departure").value("2026-12-01T08:15:00+02:00"))
                .andExpect(jsonPath("$.arrival").value("2026-12-01T09:45:00+01:00"))
                .andExpect(jsonPath("$.price.amount").value(120.5))
                .andExpect(jsonPath("$.price.currency").value("EUR"));
    }

    @Test
    @DisplayName("PUT flight replaces route, airline, times and price")
    void updateFlight() throws Exception {
        mockMvc.perform(put("/api/v1/flights/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_FLIGHT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.departureAirport.iataCode").value("OTP"))
                .andExpect(jsonPath("$.airline.icaoCode").value("AFR"))
                .andExpect(jsonPath("$.price.amount").value(120.5));
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
    @DisplayName("POST flight arriving before it departs, or with an unknown currency -> 400 listing the fields")
    void inconsistentTimesAndUnknownCurrencyAreBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        // arrival 06:00Z is before the 06:15Z (08:15+02:00) departure
                        .content(NEW_FLIGHT
                                .replace("2026-12-01T09:45:00+01:00", "2026-12-01T06:00:00Z")
                                .replace("\"EUR\"", "\"XYZ\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("arrivalAfterDeparture")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("price.knownCurrency")));
    }

    @Test
    @DisplayName("POST airport with an unknown time zone -> 400")
    void unknownTimeZoneIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"iataCode":"BBU","name":"Aurel Vlaicu","cityName":"bucuresti","timeZone":"Mars/Olympus"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("knownTimeZone")));
    }

    @Test
    @DisplayName("POST airport with an over-long city name or time zone -> 400 before any lookup")
    void overlongAirportFieldsAreBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"iataCode\":\"BBU\",\"name\":\"Aurel Vlaicu\",\"cityName\":\"%s\",\"timeZone\":\"%s\"}"
                                        .formatted("x".repeat(256), "Europe/" + "x".repeat(60))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("cityName")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("timeZone")));
    }

    @Test
    @DisplayName("POST airport with an IATA code that already exists -> 409 problem+json")
    void duplicateAirportIsConflict() throws Exception {
        mockMvc.perform(
                        post("/api/v1/airports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"iataCode\":\"OTP\",\"name\":\"Duplicate\",\"cityName\":\"bucuresti\",\"timeZone\":\"Europe/Bucharest\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("POST airport resolves its city by name, ignoring case")
    void createAirportByCityName() throws Exception {
        mockMvc.perform(
                        post("/api/v1/airports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"iataCode\":\"BBU\",\"name\":\"Aurel Vlaicu\",\"cityName\":\"bucuresti\",\"timeZone\":\"Europe/Bucharest\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.iataCode").value("BBU"))
                .andExpect(jsonPath("$.city.cityName").value("Bucuresti"))
                .andExpect(jsonPath("$.timeZone").value("Europe/Bucharest"));
    }
}
