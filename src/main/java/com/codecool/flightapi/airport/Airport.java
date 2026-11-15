package com.codecool.flightapi.airport;

import com.codecool.flightapi.city.City;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Airport")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
public class Airport {

    @Id
    @Column(name = "airport_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique (V5). */
    @Column(name = "airport_iata_code", nullable = false, unique = true, length = 3)
    private String airportIataCode;

    @Column(name = "airport_name", nullable = false)
    private String airportName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    /** IANA time zone, e.g. {@code Europe/Bucharest}; flight times are shown in it. */
    @Column(name = "time_zone", nullable = false, length = 64)
    private ZoneId timeZone;

    /** Identity is the database id; a new, unsaved airport only equals itself. Safe with Hibernate proxies. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Airport other && id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Airport.class.hashCode();
    }
}
