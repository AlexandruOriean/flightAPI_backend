package com.codecool.flightapi.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An account. Created by registration (plain users) or admin provisioning; the application never
 * changes an existing account, so there are no setters.
 */
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    /** Lower case ({@link #normalize}); unique (V1). */
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /** Password hash, never the password itself. */
    @Column(name = "password", nullable = false)
    private String password;

    /** Lower case ({@link #normalize}); unique (V1). */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Getter(AccessLevel.NONE)
    private Set<Role> roles = new HashSet<>();

    /**
     * @param username stored in lower case
     * @param password the password hash
     * @param email stored in lower case
     */
    public User(Long id, String name, String username, String password, String email, Set<Role> roles) {
        this.id = id;
        this.name = name;
        this.username = normalize(username);
        this.password = password;
        this.email = normalize(email);
        this.roles = new HashSet<>(roles);
    }

    /** The account's roles; read-only. */
    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    /**
     * Usernames and e-mails are stored and looked up in lower case (V9), so the unique constraints
     * hold regardless of case: {@code Admin} and {@code admin} are the same account.
     */
    public static String normalize(String usernameOrEmail) {
        return usernameOrEmail.toLowerCase(Locale.ROOT);
    }

    /** Identity is the database id; a new, unsaved user only equals itself. Safe with Hibernate proxies. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof User other && id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }
}
