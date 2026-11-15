package com.codecool.flightapi.city;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface CityRepository extends JpaRepository<City, Long> {

    /** The city whose name has this {@link City#key}. */
    Optional<City> findByCityNameKey(String cityNameKey);
}
