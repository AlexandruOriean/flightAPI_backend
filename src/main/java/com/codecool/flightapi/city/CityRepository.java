package com.codecool.flightapi.city;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CityRepository extends JpaRepository<City, Long> {

    /** The city whose name has this {@link City#key}. */
    Optional<City> findByNameKey(String nameKey);
}
