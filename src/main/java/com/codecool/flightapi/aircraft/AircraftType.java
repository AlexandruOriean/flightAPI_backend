package com.codecool.flightapi.aircraft;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A kind of aircraft, e.g. the Airbus A320, identified by its ICAO type designator. Flights may say which type
 * operates them. Seat counts depend on each airline's cabin layout, so {@link #typicalSeats} is informative only.
 */
@Entity
@Table(name = "aircraft_type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class AircraftType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** ICAO aircraft type designator, e.g. {@code A320}, {@code B738}. Unique (V14). */
    @Column(name = "icao_code", nullable = false, unique = true, length = 4)
    private String icaoCode;

    @Column(name = "manufacturer", nullable = false, length = 100)
    private String manufacturer;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "cruise_speed_kmh", nullable = false)
    private int cruiseSpeedKmh;

    /** Typical number of seats; airlines configure their cabins differently. Optional. */
    @Column(name = "typical_seats")
    private Integer typicalSeats;

    /** Optimistic lock: a concurrent update of the same row fails instead of silently overwriting it. */
    @Version
    @Column(name = "version", nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    public AircraftType(
            Long id, String icaoCode, String manufacturer, String model, int cruiseSpeedKmh, Integer typicalSeats) {
        this.id = id;
        this.icaoCode = icaoCode;
        this.manufacturer = manufacturer;
        this.model = model;
        this.cruiseSpeedKmh = cruiseSpeedKmh;
        this.typicalSeats = typicalSeats;
    }

    /** Identity is the database id; a new, unsaved aircraft type only equals itself. Safe with Hibernate proxies. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof AircraftType other && id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return AircraftType.class.hashCode();
    }
}
