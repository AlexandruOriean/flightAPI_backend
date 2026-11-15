package com.codecool.flightapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.codecool.flightapi.airline.Airline;
import com.codecool.flightapi.airplane.Airplane;
import com.codecool.flightapi.airport.Airport;
import com.codecool.flightapi.city.City;
import com.codecool.flightapi.flight.Flight;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Entities are equal by database id only, and keep their hash code when their fields change. */
class EntityEqualityTest {

    @Test
    @DisplayName("same id -> equal, whatever the other fields")
    void sameIdIsEqual() {
        assertThat(new City(1L, "London")).isEqualTo(new City(1L, "Londra"));
        assertThat(new Airline(1L, "Tarom", "ROT")).isEqualTo(new Airline(1L, "TAROM", "ROT"));
        assertThat(new Airplane(1L, "Airbus", "A320", 150L, 800))
                .isEqualTo(new Airplane(1L, "Boeing", "B737", 180L, 700));
        assertThat(airport(1L, "OTP")).isEqualTo(airport(1L, "BBU"));
    }

    @Test
    @DisplayName("different ids, or unsaved entities (no id) -> not equal")
    void differentOrMissingIdIsNotEqual() {
        assertThat(new City(1L, "London")).isNotEqualTo(new City(2L, "London"));
        assertThat(new City(null, "London")).isNotEqualTo(new City(null, "London"));
        assertThat(airport(null, "OTP")).isNotEqualTo(airport(null, "OTP"));
    }

    @Test
    @DisplayName("an entity stays findable in a HashSet after it is saved (id assigned) or edited")
    void hashCodeIsStable() {
        City city = new City(null, "Cluj");
        Set<City> cities = new HashSet<>(Set.of(city));

        city.setCityId(7L);
        city.setCityName("Cluj-Napoca");

        assertThat(cities).contains(city);
    }

    @Test
    @DisplayName("flights compare by id only")
    void flightsCompareById() {
        assertThat(flight(1L, LocalTime.of(9, 30))).isEqualTo(flight(1L, LocalTime.of(10, 30)));
        assertThat(flight(1L, LocalTime.of(9, 30))).isNotEqualTo(flight(2L, LocalTime.of(9, 30)));
    }

    private static Flight flight(Long id, LocalTime departure) {
        Airline tarom = new Airline(1L, "Tarom", "ROT");
        return new Flight(
                id,
                LocalDate.of(2026, 12, 1),
                100L,
                departure,
                departure.plusHours(2),
                airport(1L, "OTP"),
                airport(2L, "LHR"),
                tarom);
    }

    private static Airport airport(Long id, String iataCode) {
        return new Airport(id, iataCode, "Airport " + iataCode, new City(1L, "Bucuresti"));
    }
}
