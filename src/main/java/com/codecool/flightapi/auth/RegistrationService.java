package com.codecool.flightapi.auth;

import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.user.Role;
import com.codecool.flightapi.user.RoleName;
import com.codecool.flightapi.user.RoleRepository;
import com.codecool.flightapi.user.User;
import com.codecool.flightapi.user.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user with {@link RoleName#ROLE_USER} only. Clients cannot choose their roles.
     *
     * @throws ResourceAlreadyExistsException if the username or e-mail is already taken
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResourceAlreadyExistsException("Username is already taken.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistsException("E-mail is already registered.");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setRole(Set.of(findRole(RoleName.ROLE_USER)));
        userRepository.save(user);
    }

    private Role findRole(RoleName name) {
        return roleRepository
                .findByName(name)
                .orElseThrow(() -> new IllegalStateException("Role " + name + " is missing from the database"));
    }
}
