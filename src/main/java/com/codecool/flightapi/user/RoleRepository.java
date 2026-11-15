package com.codecool.flightapi.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(RoleName name);

    /**
     * The role with this name. Every {@link RoleName} is seeded by V2, so a missing one means a broken database.
     *
     * @throws IllegalStateException if the role is missing
     */
    default Role getByName(RoleName name) {
        return findByName(name)
                .orElseThrow(() -> new IllegalStateException("Role " + name + " is missing from the database"));
    }
}
