package com.codecool.flightapi.flight;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codecool.flightapi.airline.AirlineResponse;
import com.codecool.flightapi.airport.AirportResponse;
import com.codecool.flightapi.city.CityResponse;
import com.codecool.flightapi.common.error.InvalidRequestException;
import com.codecool.flightapi.common.money.MoneyDto;
import com.codecool.flightapi.common.web.ETags.IfMatch;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer of {@link FlightController}: routing between list and search, search parameter validation and
 * request body validation. Shared error handling is covered in {@code AircraftTypeControllerTest}.
 */
@WebMvcTest(FlightController.class)
@WithMockUser
class FlightControllerTest {

    private static final String FLIGHT_JSON = """
            {"departureAirportCode":"OTP","arrivalAirportCode":"LHR","airlineCode":"ROT",
             "departure":"2026-12-01T08:15:00+02:00","arrival":"2026-12-01T09:45:00Z",
             "price":{"amount":35.00,"currency":"EUR"},"aircraftTypeCode":"A320"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlightService flightService;

    private final FlightResponse flight = new FlightResponse(
            7L,
            OffsetDateTime.parse("2026-12-01T08:15:00+02:00"),
            OffsetDateTime.parse("2026-12-01T09:45:00Z"),
            new MoneyDto(new BigDecimal("35.00"), "EUR"),
            new AirportResponse(
                    1L, "OTP", "Henri Coanda", new CityResponse(12L, "Bucuresti", 0), "Europe/Bucharest", 0),
            new AirportResponse(2L, "LHR", "Heathrow", new CityResponse(21L, "London", 0), "Europe/London", 0),
            new AirlineResponse(1L, "Tarom", "ROT", 0),
            null,
            0);

    @Test
    @DisplayName("GET /api/v1/flights without search parameters -> one page of flights")
    void getFlights() throws Exception {
        when(flightService.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(flight), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/flights/{id} -> local times with offset, price as amount + currency")
    void getFlight() throws Exception {
        when(flightService.findById(7L)).thenReturn(flight);

        mockMvc.perform(get("/api/v1/flights/{id}", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departure").value("2026-12-01T08:15:00+02:00"))
                .andExpect(jsonPath("$.price.amount").value(35.00))
                .andExpect(jsonPath("$.price.currency").value("EUR"))
                .andExpect(jsonPath("$.price.knownCurrency").doesNotExist())
                .andExpect(jsonPath("$.airline.icaoCode").value("ROT"))
                .andExpect(jsonPath("$.aircraftType").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/flights?from=&to=&date= -> the search, one page like the list")
    void searchFlights() throws Exception {
        when(flightService.search("OTP", "London", LocalDate.of(2026, 12, 1), PageRequest.of(1, 5)))
                .thenReturn(new PageImpl<>(List.of(flight), PageRequest.of(1, 5), 6));

        mockMvc.perform(get("/api/v1/flights")
                        .param("from", "OTP")
                        .param("to", "London")
                        .param("date", "2026-12-01")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].arrivalAirport.iataCode").value("LHR"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(6));
    }

    @Test
    @DisplayName("any single search parameter routes to the search, which then needs all three -> 400")
    void partialSearchIsBadRequest() throws Exception {
        for (String parameter : FlightController.SEARCH_PARAMETERS) {
            mockMvc.perform(get("/api/v1/flights").param(parameter, "2026-12-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        verifyNoInteractions(flightService);
    }

    @Test
    @DisplayName("blank, too long or non-date search parameters -> 400 problem+json")
    void invalidSearchParametersAreBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/flights")
                        .param("from", " ")
                        .param("to", "LHR")
                        .param("date", "2026-12-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/v1/flights")
                        .param("from", "OTP")
                        .param("to", "x".repeat(101))
                        .param("date", "2026-12-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/flights")
                        .param("from", "OTP")
                        .param("to", "LHR")
                        .param("date", "01.12.2026"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(flightService);
    }

    @Test
    @DisplayName("search the service rejects (term too short) -> 400 with its message")
    void searchRejectedByTheService() throws Exception {
        when(flightService.search(eq("OT"), eq("LHR"), eq(LocalDate.of(2026, 12, 1)), any(Pageable.class)))
                .thenThrow(new InvalidRequestException("Search terms need at least 3 characters."));

        mockMvc.perform(get("/api/v1/flights")
                        .param("from", "OT")
                        .param("to", "LHR")
                        .param("date", "2026-12-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"))
                .andExpect(jsonPath("$.detail").value("Search terms need at least 3 characters."));
    }

    @Test
    @DisplayName("POST /api/v1/flights -> 201 with Location header")
    void createFlight() throws Exception {
        when(flightService.create(any(FlightRequest.class))).thenReturn(flight);

        mockMvc.perform(post("/api/v1/flights")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FLIGHT_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/flights/7"));
    }

    @Test
    @DisplayName("invalid flight: codes, same airports, arrival before departure, price, aircraft type -> 400")
    void invalidFlight() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departureAirportCode":"OTP","arrivalAirportCode":"OTP","airlineCode":"rot",
                                 "departure":"2026-12-01T10:00:00Z","arrival":"2026-12-01T09:00:00Z",
                                 "price":{"amount":-1,"currency":"XYZ"},"aircraftTypeCode":"a-320"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field")
                        .value(containsInAnyOrder(
                                "airlineCode",
                                "differentAirports",
                                "arrivalAfterDeparture",
                                "price.amount",
                                "price.knownCurrency",
                                "aircraftTypeCode")));
        verifyNoInteractions(flightService);
    }

    @Test
    @DisplayName("flight without required fields -> 400 listing each of them")
    void incompleteFlight() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field")
                        .value(containsInAnyOrder(
                                "departureAirportCode",
                                "arrivalAirportCode",
                                "airlineCode",
                                "departure",
                                "arrival",
                                "price")));
    }

    @Test
    @DisplayName("PUT /api/v1/flights/{id} -> 200 with the updated flight")
    void updateFlight() throws Exception {
        when(flightService.update(eq(7L), any(FlightRequest.class), any(IfMatch.class)))
                .thenReturn(flight);

        mockMvc.perform(put("/api/v1/flights/{id}", 7)
                        .with(csrf())
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FLIGHT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @DisplayName("DELETE /api/v1/flights/{id} -> 204")
    void deleteFlight() throws Exception {
        mockMvc.perform(delete("/api/v1/flights/{id}", 7).with(csrf())).andExpect(status().isNoContent());

        verify(flightService).delete(7L);
    }
}
