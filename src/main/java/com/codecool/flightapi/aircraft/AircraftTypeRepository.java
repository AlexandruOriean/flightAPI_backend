package com.codecool.flightapi.aircraft;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AircraftTypeRepository extends JpaRepository<AircraftType, Long> {

    Optional<AircraftType> findByIcaoCode(String icaoCode);
}
