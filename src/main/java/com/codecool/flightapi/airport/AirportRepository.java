package com.codecool.flightapi.airport;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
interface AirportRepository extends JpaRepository<Airport, Long> {

    /** The city is lazy; load it in the same query. */
    @Override
    @EntityGraph(attributePaths = "city")
    Page<Airport> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "city")
    Optional<Airport> findById(Long id);

    /** The city is lazy; load it in the same query. */
    @EntityGraph(attributePaths = "city")
    Optional<Airport> findByAirportIataCode(String airportIataCode);

    @Query("select a.airportName from Airport a order by a.airportName")
    List<String> findAllNames();
}
