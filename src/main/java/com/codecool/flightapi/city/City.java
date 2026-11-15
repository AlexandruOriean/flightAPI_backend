package com.codecool.flightapi.city;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "City")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    /** {@link #key} of the name, kept in step with it; unique (V11), so names are unique regardless of case. */
    @Column(name = "city_name_key", nullable = false, unique = true)
    @Setter(AccessLevel.NONE)
    private String cityNameKey;

    /** Optimistic lock (V10): a concurrent update of the same row fails instead of silently overwriting it. */
    @Version
    @Column(name = "version", nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    public City(Long cityId, String cityName) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.cityNameKey = key(cityName);
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
        this.cityNameKey = key(cityName);
    }

    /** How city names are compared: trimmed and in lower case, so {@code London} and {@code london } match. */
    public static String key(String cityName) {
        return cityName == null ? null : cityName.strip().toLowerCase(Locale.ROOT);
    }

    /** Identity is the database id; a new, unsaved city only equals itself. Safe with Hibernate proxies. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof City other && cityId != null && cityId.equals(other.getCityId());
    }

    @Override
    public int hashCode() {
        return City.class.hashCode();
    }
}
