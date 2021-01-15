package com.codecool.flight_api_project.cityTest;


import com.codecool.flight_api_project.airline.Airline;
import com.codecool.flight_api_project.city.City;
import com.codecool.flight_api_project.city.CityRepository;
import com.codecool.flight_api_project.city.CityService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CitiesTest {

    @Autowired
    private CityService cityService;

    @MockBean
    private CityRepository cityRepository;


    @Test
    public void getAllCitiesTest(){
        when(cityRepository.findAll()).thenReturn(
                Stream.of
                        (new City(2L,"Aachen"),
                                new City(3L,"Aalborg"),
                                new City(4L,"Aarhus"))
                .collect(Collectors.toList()));
        assertEquals(3,cityService.getAllcities().size());
    }

    @Test
    public void getCityByIdTest(){
        City city = new City(2L, "Aachen");
        Long cityId = city.getCityId();
        when(cityRepository.getOne(cityId)).thenReturn(city);
        assertEquals(city, cityService.getCityById(cityId));
    }

    @Test
    public void updateCityByIdTest(){
        City city = new City(2L,"Aachen");
        City updatedCity = new City(2L,"Amsterdam");

        doReturn(city).when(cityRepository).getOne(2L);
        doReturn(updatedCity).when(cityRepository).save(city);

        updatedCity = cityService.updateCityById(city.getCityId(),city);
        Assertions.assertEquals("Amsterdam",updatedCity.getCityName()

        );
    }

    @Test
    public void saveCityTest(){
        City city =  new City(2L,"Aachen");
        when(cityRepository.save(city)).thenReturn(city);
        assertEquals(city,cityService.saveCity(city));

    }

    @Test
    public void deleteCityByIdTest(){
        Long cityId = 2L;
        cityService.deleteCityById(cityId);
        verify(cityRepository,times(1)).deleteById(cityId);

    }



}

