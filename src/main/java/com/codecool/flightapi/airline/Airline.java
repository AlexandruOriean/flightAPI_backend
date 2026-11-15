package com.codecool.flightapi.airline;

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
@Table(name = "Airline")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    /** Optimistic lock (V10): a concurrent update of the same row fails instead of silently overwriting it. */
    @Version
    @Column(name = "version", nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    public Airline(Long id, String name, String icaoCode) {
        this.id = id;
        this.name = name;
        this.icaoCode = icaoCode;
    }

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
