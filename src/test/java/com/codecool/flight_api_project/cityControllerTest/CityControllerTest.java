package com.codecool.flight_api_project.cityControllerTest;
import org.springframework.security.test.context.support.WithMockUser;
import com.codecool.flight_api_project.city.City;
import com.codecool.flight_api_project.city.CityService;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username="user",roles = "ADMIN")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CityControllerTest {

    @MockBean
    private CityService cityService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Test all cities found - GET/ api/v1/cities")
    public void testAllCities() throws Exception{

        City city1 = new City(1L,"108 Mile");
        City city2 = new City(2L ,"Aachen");

        List<City> cityList = new ArrayList<>();
        cityList.add(city1);
        cityList.add(city2);


        doReturn(cityList).when(cityService).getAllcities();
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cities"))

                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))

//
                .andExpect(jsonPath("$[0].cityName", is("108 Mile")))
                .andExpect(jsonPath("$[1].cityName", is("Aachen")));

    }
}
