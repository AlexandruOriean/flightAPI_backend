package com.codecool.flightapi.aircraft;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer of {@link AircraftTypeController}: status codes, headers, bodies and problem+json errors. */
@WebMvcTest(AircraftTypeController.class)
@WithMockUser
class AircraftTypeControllerTest {

    private static final String AIRCRAFT_TYPE_JSON = """
            {"icaoCode":"A388","manufacturer":"Airbus","model":"A380-800","cruiseSpeedKmh":903,"typicalSeats":555}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AircraftTypeService aircraftTypeService;

    private final AircraftTypeResponse airbus = new AircraftTypeResponse(1L, "A388", "Airbus", "A380-800", 903, 555);
    private final AircraftTypeResponse boeing = new AircraftTypeResponse(2L, "B38M", "Boeing", "737 MAX 8", 840, null);

    @Test
    @DisplayName("GET /api/v1/aircraft-types -> 200 with one page of aircraft types and the page metadata")
    void getAircraftTypes() throws Exception {
        when(aircraftTypeService.findAll(PageRequest.of(1, 2)))
                .thenReturn(new PageImpl<>(List.of(airbus, boeing), PageRequest.of(1, 2), 5));

        mockMvc.perform(get("/api/v1/aircraft-types").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].icaoCode").value("A388"))
                .andExpect(jsonPath("$.content[1].manufacturer").value("Boeing"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/aircraft-types without parameters asks for the first 20")
    void getAircraftTypesDefaultsToFirstPage() throws Exception {
        when(aircraftTypeService.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/aircraft-types")).andExpect(status().isOk());

        verify(aircraftTypeService).findAll(PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("GET /api/v1/aircraft-types caps the page size at 100")
    void getAircraftTypesCapsThePageSize() throws Exception {
        when(aircraftTypeService.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/aircraft-types").param("size", "100000")).andExpect(status().isOk());

        verify(aircraftTypeService).findAll(PageRequest.of(0, 100));
    }

    @Test
    @DisplayName("GET /api/v1/aircraft-types/{id} -> 200 with the aircraft type")
    void getAircraftType() throws Exception {
        when(aircraftTypeService.findById(1L)).thenReturn(airbus);

        mockMvc.perform(get("/api/v1/aircraft-types/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.icaoCode").value("A388"));
    }

    @Test
    @DisplayName("GET /api/v1/aircraft-types/{id} for an unknown id -> 404 problem+json")
    void getUnknownAircraftType() throws Exception {
        when(aircraftTypeService.findById(99L)).thenThrow(ResourceNotFoundException.byId("Aircraft type", 99L));

        mockMvc.perform(get("/api/v1/aircraft-types/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Aircraft type with id '99' was not found"))
                .andExpect(jsonPath("$.instance").value("/api/v1/aircraft-types/99"));
    }

    @Test
    @DisplayName("POST /api/v1/aircraft-types -> 201 with Location header and the created aircraft type")
    void createAircraftType() throws Exception {
        when(aircraftTypeService.create(any(AircraftTypeRequest.class))).thenReturn(airbus);

        mockMvc.perform(post("/api/v1/aircraft-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AIRCRAFT_TYPE_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/aircraft-types/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/aircraft-types binds the request DTO; a client-supplied id is not part of it")
    void createAircraftTypeBindsRequest() throws Exception {
        when(aircraftTypeService.create(any(AircraftTypeRequest.class))).thenReturn(airbus);

        mockMvc.perform(post("/api/v1/aircraft-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":99,"icaoCode":"A388","manufacturer":"Airbus","model":"A380-800","cruiseSpeedKmh":903,"typicalSeats":555}
                                """))
                .andExpect(status().isCreated());

        verify(aircraftTypeService).create(new AircraftTypeRequest("A388", "Airbus", "A380-800", 903, 555));
    }

    @Test
    @DisplayName("invalid request body -> 400 problem+json listing every invalid field")
    void invalidRequestIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"icaoCode":"a380x","manufacturer":"","model":"A380","typicalSeats":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field")
                        .value(containsInAnyOrder("icaoCode", "manufacturer", "cruiseSpeedKmh", "typicalSeats")));
        verifyNoInteractions(aircraftTypeService);
    }

    @Test
    @DisplayName("PUT /api/v1/aircraft-types/{id} -> 200 with the updated aircraft type")
    void updateAircraftType() throws Exception {
        when(aircraftTypeService.update(eq(1L), any(AircraftTypeRequest.class))).thenReturn(airbus);

        mockMvc.perform(put("/api/v1/aircraft-types/{id}", 1)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AIRCRAFT_TYPE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icaoCode").value("A388"));
    }

    @Test
    @DisplayName("DELETE /api/v1/aircraft-types/{id} -> 204 without body")
    void deleteAircraftType() throws Exception {
        mockMvc.perform(delete("/api/v1/aircraft-types/{id}", 1).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(aircraftTypeService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/aircraft-types/{id} for an unknown id -> 404 problem+json")
    void deleteUnknownAircraftType() throws Exception {
        doThrow(ResourceNotFoundException.byId("Aircraft type", 99L))
                .when(aircraftTypeService)
                .delete(99L);

        mockMvc.perform(delete("/api/v1/aircraft-types/{id}", 99).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("malformed JSON body -> 400 problem+json")
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("non-numeric id -> 400 problem+json instead of 500")
    void nonNumericIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/aircraft-types/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("concurrent update of the same aircraft type -> 409 problem+json")
    @WithMockUser(roles = "ADMIN")
    void concurrentUpdateIsConflict() throws Exception {
        when(aircraftTypeService.update(eq(1L), any(AircraftTypeRequest.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(AircraftType.class, 1L));

        mockMvc.perform(
                        put("/api/v1/aircraft-types/{id}", 1)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"icaoCode\":\"A320\",\"manufacturer\":\"Airbus\",\"model\":\"A320\",\"cruiseSpeedKmh\":840}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Concurrent update"));
    }

    @Test
    @DisplayName("unexpected exception -> 500 problem+json without internal details")
    void unexpectedErrorDoesNotLeakDetails() throws Exception {
        when(aircraftTypeService.findAll(any(Pageable.class)))
                .thenThrow(new IllegalStateException("db password is hunter2"));

        mockMvc.perform(get("/api/v1/aircraft-types"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }
}
