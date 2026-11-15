package com.codecool.flightapi.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Against the real Flyway schema (embedded H2). */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void findsTheUsernamesHoldingARoleAlphabetically() {
        Role user = roleRepository.getByName(RoleName.ROLE_USER);
        Role admin = roleRepository.getByName(RoleName.ROLE_ADMIN);
        save("zed", Set.of(user, admin));
        save("anna", Set.of(user));
        save("bob", Set.of(admin));

        assertThat(userRepository.findUsernamesByRole(RoleName.ROLE_ADMIN)).containsExactly("bob", "zed");
        assertThat(userRepository.findUsernamesByRole(RoleName.ROLE_USER)).containsExactly("anna", "zed");
    }

    private void save(String username, Set<Role> roles) {
        userRepository.save(new User(null, username, username, "{bcrypt}hash", username + "@example.com", roles));
    }
}
