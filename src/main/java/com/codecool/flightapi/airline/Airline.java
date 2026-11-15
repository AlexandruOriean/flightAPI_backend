package com.codecool.flightapi.airline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Airline")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    /** Three-letter ICAO airline designator, e.g. {@code ROT}. Unique (V5). */
    @Column(name = "icao_code", nullable = false, unique = true)
    private String icaoCode;

    /** Identity is the database id; a new, unsaved airline only equals itself. Safe with Hibernate proxies. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Airline other && id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Airline.class.hashCode();
    }
}
