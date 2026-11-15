package com.codecool.flightapi.airplane;

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

@Entity
@Table(name = "Airplane")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class Airplane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "manufacturer", nullable = false)
    private String manufacturer;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "number_of_seats", nullable = false)
    private Long numberOfSeats;

    /** Cruise speed in km/h. */
    @Column(name = "speed", nullable = false)
    private double speed;

    /** Optimistic lock (V10): a concurrent update of the same row fails instead of silently overwriting it. */
    @Version
    @Column(name = "version", nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    public Airplane(Long id, String manufacturer, String model, Long numberOfSeats, double speed) {
        this.id = id;
        this.manufacturer = manufacturer;
        this.model = model;
        this.numberOfSeats = numberOfSeats;
        this.speed = speed;
    }

    /** Identity is the database id; a new, unsaved airplane only equals itself. Safe with Hibernate proxies. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Airplane other && id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Airplane.class.hashCode();
    }
}
