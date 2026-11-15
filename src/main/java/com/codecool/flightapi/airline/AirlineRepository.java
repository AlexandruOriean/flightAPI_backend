package com.codecool.flightapi.airline;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AirlineRepository extends JpaRepository<Airline, Long> {

    Optional<Airline> findByIcaoCode(String icaoCode);
}
