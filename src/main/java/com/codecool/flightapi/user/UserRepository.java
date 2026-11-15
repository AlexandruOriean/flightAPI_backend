package com.codecool.flightapi.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> getByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
