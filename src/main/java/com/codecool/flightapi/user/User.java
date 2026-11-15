package com.codecool.flightapi.user;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.*;
import java.util.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> role;

    /**
     * Usernames and e-mails are stored and looked up in lower case (V9), so the unique constraints
     * hold regardless of case: {@code Admin} and {@code admin} are the same account.
     */
    public static String normalize(String usernameOrEmail) {
        return usernameOrEmail.toLowerCase(Locale.ROOT);
    }
}
