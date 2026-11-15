package com.codecool.flightapi.airport;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codecool.flightapi.city.CityResponse;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer of {@link AirportController}; shared error handling is covered in {@code AircraftTypeControllerTest}. */
@WebMvcTest(AirportController.class)
@WithMockUser
class AirportControllerTest {

    private static final String OTOPENI_JSON = """
            {"iataCode":"OTP","name":"Henri Coanda","cityName":"Bucuresti","timeZone":"Europe/Bucharest"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AirportService airportService;

    private final AirportResponse otopeni =
            new AirportResponse(1L, "OTP", "Henri Coanda", new CityResponse(12L, "Bucuresti"), "Europe/Bucharest");

    @Test
    @DisplayName("GET /api/v1/airports -> 200 with one page of airports and their cities")
    void getAirports() throws Exception {
        when(airportService.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(otopeni), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].iataCode").value("OTP"))
                .andExpect(jsonPath("$.content[0].city.name").value("Bucuresti"))
                .andExpect(jsonPath("$.content[0].timeZone").value("Europe/Bucharest"));
    }

    @Test
    @DisplayName("GET /api/v1/airports/{id} -> 200 with the airport")
    void getAirport() throws Exception {
        when(airportService.findById(1L)).thenReturn(otopeni);

        mockMvc.perform(get("/api/v1/airports/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Henri Coanda"));
    }

    @Test
    @DisplayName("GET /api/v1/airports/iata/{code} -> 200; unknown code -> 404")
    void getAirportByIataCode() throws Exception {
        when(airportService.findByIataCode("otp")).thenReturn(otopeni);
        when(airportService.findByIataCode("XXX"))
                .thenThrow(new ResourceNotFoundException("Airport", "IATA code", "XXX"));

        mockMvc.perform(get("/api/v1/airports/iata/{code}", "otp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get("/api/v1/airports/iata/{code}", "XXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Airport with IATA code 'XXX' was not found"));
    }

    @Test
    @DisplayName("GET /api/v1/airports/names -> 200 with a plain list of names")
    void getAirportNames() throws Exception {
        when(airportService.findAllNames()).thenReturn(List.of("Heathrow", "Henri Coanda"));

        mockMvc.perform(get("/api/v1/airports/names"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Heathrow"));
    }

    @Test
    @DisplayName("POST /api/v1/airports -> 201 with Location header")
    void createAirport() throws Exception {
        when(airportService.create(new AirportRequest("OTP", "Henri Coanda", "Bucuresti", "Europe/Bucharest")))
                .thenReturn(otopeni);

        mockMvc.perform(post("/api/v1/airports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OTOPENI_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/airports/1"));
    }

    @Test
    @DisplayName("POST /api/v1/airports with an unknown city -> 404")
    void createAirportInUnknownCity() throws Exception {
        when(airportService.create(any(AirportRequest.class)))
                .thenThrow(new ResourceNotFoundException("City", "name", "Bucuresti"));

        mockMvc.perform(post("/api/v1/airports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OTOPENI_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("invalid airport, including an offset instead of an IANA zone -> 400 listing every field")
    void invalidAirport() throws Exception {
        mockMvc.perform(post("/api/v1/airports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"iataCode":"OT","name":"","cityName":" ","timeZone":"+02:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field")
                        .value(containsInAnyOrder("iataCode", "name", "cityName", "knownTimeZone")));
        verifyNoInteractions(airportService);
    }

    @Test
    @DisplayName("PUT /api/v1/airports/{id} -> 200 with the updated airport")
    void updateAirport() throws Exception {
        when(airportService.update(eq(1L), any(AirportRequest.class))).thenReturn(otopeni);

        mockMvc.perform(put("/api/v1/airports/{id}", 1)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OTOPENI_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/airports/{id} -> 204")
    void deleteAirport() throws Exception {
        mockMvc.perform(delete("/api/v1/airports/{id}", 1).with(csrf())).andExpect(status().isNoContent());

        verify(airportService).delete(1L);
    }
}
