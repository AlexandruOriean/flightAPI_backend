package com.codecool.flightapi.flight;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface FlightRepository extends JpaRepository<Flight, Long> {

    /** Airports, their cities and the airline are lazy; load them in the same query. */
    @Override
    @EntityGraph(attributePaths = {"departureAirport.city", "arrivalAirport.city", "airline"})
    Page<Flight> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"departureAirport.city", "arrivalAirport.city", "airline"})
    Optional<Flight> findById(Long id);

    /**
     * Flights departing in {@code [from, until)} between two airports, each matched by exact IATA code or by a LIKE
     * pattern on the airport or city name. Airports, cities and airline are fetched in the same query.
     *
     * @param fromCode upper-case IATA code of the departure airport
     * @param fromPattern lower-case LIKE pattern ({@code \} escapes) for the departure airport/city name
     * @param toCode upper-case IATA code of the arrival airport
     * @param toPattern lower-case LIKE pattern ({@code \} escapes) for the arrival airport/city name
     * @param from start of the departure window, inclusive
     * @param until end of the departure window, exclusive
     * @param limit maximum number of flights returned, earliest departures first
     */
    @Query("""
            select f from Flight f
              join fetch f.departureAirport dep left join fetch dep.city depCity
              join fetch f.arrivalAirport arr left join fetch arr.city arrCity
              left join fetch f.airline
            where f.departureAt >= :from and f.departureAt < :until
              and (dep.airportIataCode = :fromCode
                   or lower(dep.airportName) like :fromPattern escape '\\'
                   or lower(depCity.cityName) like :fromPattern escape '\\')
              and (arr.airportIataCode = :toCode
                   or lower(arr.airportName) like :toPattern escape '\\'
                   or lower(arrCity.cityName) like :toPattern escape '\\')
            order by f.departureAt
            """)
    List<Flight> search(
            @Param("fromCode") String fromCode,
            @Param("fromPattern") String fromPattern,
            @Param("toCode") String toCode,
            @Param("toPattern") String toPattern,
            @Param("from") Instant from,
            @Param("until") Instant until,
            Limit limit);
}
