package com.codecool.flightapi.airline;

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

import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
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

/**
 * Web layer of {@link AirlineController}. Error handling shared by all controllers (malformed JSON, bad ids,
 * 500s) is covered once in {@code AircraftTypeControllerTest}; access rules in {@code AccessRulesIT}.
 */
@WebMvcTest(AirlineController.class)
@WithMockUser
class AirlineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AirlineService airlineService;

    private final AirlineResponse tarom = new AirlineResponse(1L, "Tarom", "ROT");

    @Test
    @DisplayName("GET /api/v1/airlines -> 200 with one page of airlines")
    void getAirlines() throws Exception {
        when(airlineService.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(tarom), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/airlines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].icaoCode").value("ROT"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/airlines/{id} -> 200 with the airline")
    void getAirline() throws Exception {
        when(airlineService.findById(1L)).thenReturn(tarom);

        mockMvc.perform(get("/api/v1/airlines/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tarom"));
    }

    @Test
    @DisplayName("GET /api/v1/airlines/{id} for an unknown id -> 404 problem+json")
    void getUnknownAirline() throws Exception {
        when(airlineService.findById(9L)).thenThrow(ResourceNotFoundException.byId("Airline", 9L));

        mockMvc.perform(get("/api/v1/airlines/{id}", 9))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Airline with id '9' was not found"));
    }

    @Test
    @DisplayName("POST /api/v1/airlines -> 201 with Location header")
    void createAirline() throws Exception {
        when(airlineService.create(new AirlineRequest("Tarom", "ROT"))).thenReturn(tarom);

        mockMvc.perform(post("/api/v1/airlines")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tarom\",\"icaoCode\":\"ROT\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/airlines/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/airlines with a taken ICAO code -> 409 problem+json")
    void createDuplicateAirline() throws Exception {
        when(airlineService.create(any(AirlineRequest.class)))
                .thenThrow(new ResourceAlreadyExistsException("An airline with ICAO code 'ROT' already exists."));

        mockMvc.perform(post("/api/v1/airlines")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tarom\",\"icaoCode\":\"ROT\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource already exists"));
    }

    @Test
    @DisplayName("invalid airline -> 400 listing name and icaoCode")
    void invalidAirline() throws Exception {
        mockMvc.perform(post("/api/v1/airlines")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"icaoCode\":\"rot\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("name", "icaoCode")));
        verifyNoInteractions(airlineService);
    }

    @Test
    @DisplayName("PUT /api/v1/airlines/{id} -> 200 with the updated airline")
    void updateAirline() throws Exception {
        when(airlineService.update(eq(1L), any(AirlineRequest.class))).thenReturn(tarom);

        mockMvc.perform(put("/api/v1/airlines/{id}", 1)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tarom\",\"icaoCode\":\"ROT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icaoCode").value("ROT"));
    }

    @Test
    @DisplayName("DELETE /api/v1/airlines/{id} -> 204")
    void deleteAirline() throws Exception {
        mockMvc.perform(delete("/api/v1/airlines/{id}", 1).with(csrf())).andExpect(status().isNoContent());

        verify(airlineService).delete(1L);
    }
}
