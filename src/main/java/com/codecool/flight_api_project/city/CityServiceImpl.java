package com.codecool.flight_api_project.city;

import com.codecool.flight_api_project.airline.Airline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Repository
public class CityServiceImpl implements CityService {

    @Autowired
    private CityRepository cityRepository;

    @Override
    public City saveCity(City city){
        return cityRepository.save(city);
    }

    @Override
    public void deleteCityById(Long id){
        cityRepository.deleteById(id);
    }

    @Override
    public City updateCityById(Long id, City cityToUpdate) {
        City city = cityRepository.getOne(id);
        city.setCityName(cityToUpdate.getCityName());
        return cityRepository.save(city);
    }

    @Override
    public List<City> getAllcities() {
       List<City> cityList = cityRepository.findAll();
       return cityList;
    }

    @Override
    public City getCityById(Long id) {
        City city = cityRepository.getOne(id);
        return city;
    }
}

