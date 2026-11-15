package com.codecool.flightapi.auth;

import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.user.AdminAccountProperties;
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
    private final AdminAccountProperties admin;

    /**
     * Registers a new user with {@link RoleName#ROLE_USER} only. Clients cannot choose their roles.
     * Username and e-mail are stored in lower case; the configured admin username cannot be registered.
     *
     * @throws ResourceAlreadyExistsException if the username or e-mail is already taken
     */
    @Transactional
    public void register(RegisterRequest request) {
        String username = User.normalize(request.username());
        String email = User.normalize(request.email());
        if (admin.isReserved(username) || userRepository.existsByUsername(username)) {
            throw new ResourceAlreadyExistsException("Username is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException("E-mail is already registered.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
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
