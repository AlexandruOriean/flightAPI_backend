package com.codecool.flightapi.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> getByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Usernames of every account holding the role, alphabetically. */
    @Query("select u.username from User u join u.roles r where r.name = :role order by u.username")
    List<String> findUsernamesByRole(@Param("role") RoleName role);
}
