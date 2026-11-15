package com.codecool.flightapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codecool.flightapi.airline.Airline;
import com.codecool.flightapi.airplane.Airplane;
import com.codecool.flightapi.airport.Airport;
import com.codecool.flightapi.city.City;
import com.codecool.flightapi.common.money.Money;
import com.codecool.flightapi.flight.Flight;
import com.codecool.flightapi.user.Role;
import com.codecool.flightapi.user.RoleName;
import com.codecool.flightapi.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
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
        assertThat(new Role(1, RoleName.ROLE_USER)).isEqualTo(new Role(1, RoleName.ROLE_ADMIN));
        assertThat(user(1L, "ana")).isEqualTo(user(1L, "bob"));
    }

    @Test
    @DisplayName("different ids, or unsaved entities (no id) -> not equal")
    void differentOrMissingIdIsNotEqual() {
        assertThat(new City(1L, "London")).isNotEqualTo(new City(2L, "London"));
        assertThat(new City(null, "London")).isNotEqualTo(new City(null, "London"));
        assertThat(airport(null, "OTP")).isNotEqualTo(airport(null, "OTP"));
        assertThat(new Role(1, RoleName.ROLE_USER)).isNotEqualTo(new Role(2, RoleName.ROLE_USER));
        assertThat(user(null, "ana")).isNotEqualTo(user(null, "ana"));
    }

    @Test
    @DisplayName("a user's roles are a set by role id, and read-only")
    void userRolesAreAReadOnlySet() {
        User user = new User(
                1L,
                "Ana",
                "ana",
                "{bcrypt}hash",
                "ana@example.com",
                Set.of(new Role(1, RoleName.ROLE_USER), new Role(2, RoleName.ROLE_ADMIN)));

        assertThat(user.getRoles()).contains(new Role(1, RoleName.ROLE_USER)).hasSize(2);
        assertThatThrownBy(() -> user.getRoles().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("username and e-mail are stored in lower case")
    void userNormalizesUsernameAndEmail() {
        User user = new User(null, "Ana", "Ana.Pop", "{bcrypt}hash", "Ana@Example.com", Set.of());

        assertThat(user.getUsername()).isEqualTo("ana.pop");
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
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
        assertThat(flight(1L, Instant.parse("2026-12-01T07:30:00Z")))
                .isEqualTo(flight(1L, Instant.parse("2026-12-01T08:30:00Z")));
        assertThat(flight(1L, Instant.parse("2026-12-01T07:30:00Z")))
                .isNotEqualTo(flight(2L, Instant.parse("2026-12-01T07:30:00Z")));
    }

    private static Flight flight(Long id, Instant departure) {
        return new Flight(
                id,
                departure,
                departure.plusSeconds(3 * 3600),
                new Money(new BigDecimal("100.00"), Currency.getInstance("RON")),
                airport(1L, "OTP"),
                airport(2L, "LHR"),
                new Airline(1L, "Tarom", "ROT"));
    }

    private static Airport airport(Long id, String iataCode) {
        return new Airport(
                id, iataCode, "Airport " + iataCode, new City(1L, "Bucuresti"), ZoneId.of("Europe/Bucharest"));
    }

    private static User user(Long id, String username) {
        return new User(id, username, username, "{bcrypt}hash", username + "@example.com", Set.of());
    }
}
