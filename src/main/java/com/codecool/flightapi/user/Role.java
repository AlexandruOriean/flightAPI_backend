package com.codecool.flightapi.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Reference data seeded by V2 ({@code ROLE_USER}, {@code ROLE_ADMIN}); read-only for the application. */
@Entity
@Table(name = "roles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Unique (V1). */
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private RoleName name;

    public Role(Integer id, RoleName name) {
        this.id = id;
        this.name = name;
    }

    /** Identity is the database id; a new, unsaved role only equals itself. Safe with Hibernate proxies. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Role other && id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Role.class.hashCode();
    }
}
