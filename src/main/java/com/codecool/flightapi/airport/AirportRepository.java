package com.codecool.flightapi.airport;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface AirportRepository extends JpaRepository<Airport, Long> {

    /** The city is lazy; load it in the same query. */
    @Override
    @EntityGraph(attributePaths = "city")
    List<Airport> findAll();

    @Override
    @EntityGraph(attributePaths = "city")
    Optional<Airport> findById(Long id);

    @Query(value = "SELECT AIRPORT_NAME FROM Airport", nativeQuery = true)
    List<String> getAllAirportsNames();

    @Query(value = "select * from AIRPORT where AIRPORT_IATA_CODE = UPPER(:iata_code)", nativeQuery = true)
    Airport getAirportByIataCode(@Param("iata_code") String iata_code);
}
