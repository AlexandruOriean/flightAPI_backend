package com.codecool.flight_api_project.city;

import com.codecool.flight_api_project.airline.Airline;

import java.util.List;

public interface CityService {

    List<City> getAllcities();

    City saveCity(City city);

    void deleteCityById(Long id);

    City updateCityById(Long id,City cityToUpdate);

    City getCityById(Long id);

}
