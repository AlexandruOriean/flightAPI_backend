//package com.codecool.flight_api_project.airplane;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.doReturn;
//import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@WebMvcTest(AirplaneController.class)
//class AirplaneControllerTest {
//
//    @MockBean
//    private AirplaneService airplaneService;
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @BeforeEach
//    void setUp() {
//    }
//
//    @Test
//    @DisplayName("Test airplanes found - GET /airplanes")
//    void getAirplanes() {
//        Airplane mockAirplaneOne = new Airplane(1L, "Airbus", "A380", 853L, 1185);
//        Airplane mockAirplaneTwo = new Airplane(2L, "Boeing", "737MAX", 180L, 800);
//
//        doReturn(mockAirplaneOne).when(airplaneService.).findById(mockAirplaneOne.getId());
//
//        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/airplanes/{id}", 1))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
//    }
//
//    @Test
//    void getAirplaneBy() {
//    }
//
//    @Test
//    void addAirplane() {
//    }
//
//    @Test
//    void deleteAirplaneById() {
//    }
//
//    @Test
//    void updateAirplaneById() {
//    }
//}