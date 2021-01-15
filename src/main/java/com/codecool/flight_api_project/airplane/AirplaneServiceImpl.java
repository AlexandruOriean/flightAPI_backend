package com.codecool.flight_api_project.airplane;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AirplaneServiceImpl implements AirplaneService {

    private static final Logger LOGGER = LogManager.getLogger(AirplaneServiceImpl.class);

    private final AirplaneRepository airplaneRepository;

    @Autowired
    public AirplaneServiceImpl(AirplaneRepository airplaneRepository) {
        this.airplaneRepository = airplaneRepository;
    }

    @Override
    public Airplane addAirplane(Airplane airplane) {
        LOGGER.info("Adding new airplane:{}", airplane.getManufacturer() + " - " + airplane.getModel());
        return airplaneRepository.save(airplane);
    }

    @Override
    public List<Airplane> getAllAirplanes() {
        return airplaneRepository.findAll();
    }

    @Override
    public Airplane getAirplaneById(Long id) {
        LOGGER.info("Finding airplane by id:{}", id);
        return airplaneRepository.findAirplaneById(id);
    }

    @Override
    public void deleteAirplaneByID(Long id) {
//        LOGGER.info("Deleting airplane with id:{}", id);
//        Airplane existingAirplane = airplaneRepository.findAirplaneById(id);
//
//        if (existingAirplane != null) {
            airplaneRepository.deleteById(id);
//        } else {
//            LOGGER.error("Airplane with id {} could not be found!", id);
//        }
    }

    @Override
    public Airplane updateAirplaneById(Long id, Airplane airplaneToUpdate) {

        LOGGER.info("Updating airplane with id:{}", id);

        Airplane existingAirplane = airplaneRepository.findAirplaneById(id);

        if (existingAirplane != null){
            existingAirplane.setManufacturer(airplaneToUpdate.getManufacturer());
            existingAirplane.setModel(airplaneToUpdate.getModel());
            existingAirplane.setNumberOfSeats(airplaneToUpdate.getNumberOfSeats());
            existingAirplane.setSpeed(airplaneToUpdate.getNumberOfSeats());
            existingAirplane = airplaneRepository.save(existingAirplane);

        } else {
            LOGGER.error("Airplane with id {} could not be updated!", id);
        }
        return existingAirplane;
    }
}
