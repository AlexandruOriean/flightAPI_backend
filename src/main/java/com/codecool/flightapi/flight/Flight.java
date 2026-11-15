package com.codecool.flightapi.flight;

import com.codecool.flightapi.aircraft.AircraftType;
import com.codecool.flightapi.airline.Airline;
import com.codecool.flightapi.airport.Airport;
import com.codecool.flightapi.common.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Flight")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_id")
    private Long id;

    /** Point in time; shown in the departure airport's time zone. Changed only by {@link #reschedule}. */
    @Column(name = "departure_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant departureAt;

    /** Point in time; shown in the arrival airport's time zone. Changed only by {@link #reschedule}. */
    @Column(name = "arrival_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant arrivalAt;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "price_amount", nullable = false, precision = 12, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "price_currency", nullable = false, length = 3))
    private Money price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departure_airport_id", nullable = false)
    private Airport departureAirport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arrival_airport_id", nullable = false)
    private Airport arrivalAirport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    /** Which kind of aircraft operates the flight; optional (V15). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_type_id")
    private AircraftType aircraftType;

    /** Optimistic lock (V10): a concurrent update of the same row fails instead of silently overwriting it. */
    @Version
    @Column(name = "version", nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    public Flight(
            Long id,
            Instant departureAt,
            Instant arrivalAt,
            Money price,
            Airport departureAirport,
            Airport arrivalAirport,
            Airline airline) {
        requireArrivalAfterDeparture(departureAt, arrivalAt);
        this.id = id;
        this.departureAt = departureAt;
        this.arrivalAt = arrivalAt;
        this.price = price;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.airline = airline;
    }

    /**
     * Sets both times together, so a flight never arrives before (or when) it departs. Requests are validated
     * before they get here, and the {@code V12} CHECK constraint backs this up in the database.
     *
     * @throws IllegalArgumentException if arrival is not after departure
     */
    public void reschedule(Instant departureAt, Instant arrivalAt) {
        requireArrivalAfterDeparture(departureAt, arrivalAt);
        this.departureAt = departureAt;
        this.arrivalAt = arrivalAt;
    }

    private static void requireArrivalAfterDeparture(Instant departureAt, Instant arrivalAt) {
        Objects.requireNonNull(departureAt, "departureAt");
        Objects.requireNonNull(arrivalAt, "arrivalAt");
        if (!arrivalAt.isAfter(departureAt)) {
            throw new IllegalArgumentException(
                    "Arrival " + arrivalAt + " must be after departure " + departureAt + ".");
        }
    }

    /** Identity is the database id; a new, unsaved flight only equals itself. Safe with Hibernate proxies. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Flight other && id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Flight.class.hashCode();
    }
}
