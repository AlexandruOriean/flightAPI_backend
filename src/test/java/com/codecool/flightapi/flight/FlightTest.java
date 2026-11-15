package com.codecool.flightapi.flight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codecool.flightapi.airline.Airline;
import com.codecool.flightapi.airport.Airport;
import com.codecool.flightapi.city.City;
import com.codecool.flightapi.common.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightTest {

    private static final Instant NINE = Instant.parse("2026-12-01T09:00:00Z");
    private static final Instant ELEVEN = Instant.parse("2026-12-01T11:00:00Z");

    @Test
    @DisplayName("reschedule sets both times")
    void rescheduleSetsBothTimes() {
        Flight flight = flight(NINE, ELEVEN);

        flight.reschedule(ELEVEN, ELEVEN.plusSeconds(3600));

        assertThat(flight.getDepartureAt()).isEqualTo(ELEVEN);
        assertThat(flight.getArrivalAt()).isEqualTo(ELEVEN.plusSeconds(3600));
    }

    @Test
    @DisplayName("reschedule rejects an arrival at or before departure and keeps the old times")
    void rescheduleRejectsArrivalNotAfterDeparture() {
        Flight flight = flight(NINE, ELEVEN);

        assertThatThrownBy(() -> flight.reschedule(ELEVEN, NINE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be after departure");
        assertThatThrownBy(() -> flight.reschedule(ELEVEN, ELEVEN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> flight.reschedule(null, ELEVEN)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> flight.reschedule(NINE, null)).isInstanceOf(NullPointerException.class);

        assertThat(flight.getDepartureAt()).isEqualTo(NINE);
        assertThat(flight.getArrivalAt()).isEqualTo(ELEVEN);
    }

    @Test
    @DisplayName("a new flight cannot be built arriving before it departs")
    void constructorEnforcesTheRule() {
        assertThatThrownBy(() -> flight(ELEVEN, NINE)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Flight flight(Instant departure, Instant arrival) {
        City city = new City(1L, "Bucuresti");
        return new Flight(
                null,
                departure,
                arrival,
                new Money(new BigDecimal("100.00"), Currency.getInstance("RON")),
                new Airport(1L, "OTP", "Henri Coanda", city, ZoneId.of("Europe/Bucharest")),
                new Airport(2L, "LHR", "Heathrow", new City(2L, "London"), ZoneId.of("Europe/London")),
                new Airline(1L, "Tarom", "ROT"));
    }
}
