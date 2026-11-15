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
    private Long id;

    @Column(name = "city_name", nullable = false)
    private String name;

    /** {@link #key} of the name, kept in step with it; unique (V11), so names are unique regardless of case. */
    @Column(name = "city_name_key", nullable = false, unique = true)
    @Setter(AccessLevel.NONE)
    private String nameKey;

    /** Optimistic lock (V10): a concurrent update of the same row fails instead of silently overwriting it. */
    @Version
    @Column(name = "version", nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    public City(Long id, String name) {
        this.id = id;
        this.name = name;
        this.nameKey = key(name);
    }

    public void setName(String name) {
        this.name = name;
        this.nameKey = key(name);
    }

    /** How city names are compared: trimmed and in lower case, so {@code London} and {@code london } match. */
    public static String key(String name) {
        return name == null ? null : name.strip().toLowerCase(Locale.ROOT);
    }

    /** Identity is the database id; a new, unsaved city only equals itself. Safe with Hibernate proxies. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof City other && id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return City.class.hashCode();
    }
}
