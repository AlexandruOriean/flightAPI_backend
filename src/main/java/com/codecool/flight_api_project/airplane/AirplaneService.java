package com.codecool.flight_api_project.airplane;

import java.util.List;
import java.util.Optional;

public interface AirplaneService {

    List<Airplane> getAllAirplanes();

    Optional<Airplane> getAirplaneById(Long id);

    Airplane addAirplane(Airplane airplane);

    void deleteAirplaneByID(Long id);

    Airplane updateAirplaneById(Long id, Airplane airplaneToUpdate);
}
