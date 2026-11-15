package com.codecool.flightapi.city;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer of {@link CityController}; shared error handling is covered in {@code AircraftTypeControllerTest}. */
@WebMvcTest(CityController.class)
@WithMockUser
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CityService cityService;

    private final CityResponse london = new CityResponse(21L, "London");

    @Test
    @DisplayName("GET /api/v1/cities -> 200 with one page of cities")
    void getCities() throws Exception {
        when(cityService.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(london), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(21))
                .andExpect(jsonPath("$.content[0].cityName").value("London"));
    }

    @Test
    @DisplayName("GET /api/v1/cities/{id} -> 200 with the city")
    void getCity() throws Exception {
        when(cityService.findById(21L)).thenReturn(london);

        mockMvc.perform(get("/api/v1/cities/{id}", 21))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityName").value("London"));
    }

    @Test
    @DisplayName("POST /api/v1/cities -> 201 with Location header")
    void createCity() throws Exception {
        when(cityService.create(new CityRequest("London"))).thenReturn(london);

        mockMvc.perform(post("/api/v1/cities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityName\":\"London\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/cities/21"));
    }

    @Test
    @DisplayName("blank or too long city name -> 400")
    void invalidCity() throws Exception {
        mockMvc.perform(post("/api/v1/cities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityName\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("cityName"));
        mockMvc.perform(post("/api/v1/cities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cityService);
    }

    @Test
    @DisplayName("PUT /api/v1/cities/{id} -> 200 with the updated city")
    void updateCity() throws Exception {
        when(cityService.update(eq(21L), any(CityRequest.class))).thenReturn(london);

        mockMvc.perform(put("/api/v1/cities/{id}", 21)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityName\":\"London\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE of a city that airports still use -> 409 problem+json without SQL details")
    void deleteReferencedCity() throws Exception {
        doThrow(new DataIntegrityViolationException("FK_AIRPORT_CITY violates constraint"))
                .when(cityService)
                .delete(21L);

        mockMvc.perform(delete("/api/v1/cities/{id}", 21).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Data integrity violation"))
                .andExpect(content().string(not(containsString("FK_AIRPORT_CITY"))));
    }
}
