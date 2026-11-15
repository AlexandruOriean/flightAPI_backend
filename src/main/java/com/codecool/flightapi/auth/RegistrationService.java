package com.codecool.flightapi.auth;

import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.user.AdminAccountProperties;
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

    /**
     * Deliberately vague. It still lets someone who picks an unused username learn whether an e-mail is
     * registered; only e-mail verification can hide that (roadmap task 32).
     */
    static final String ALREADY_IN_USE = "Username or e-mail is already in use.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAccountProperties admin;

    /**
     * Registers a new user with {@link RoleName#ROLE_USER} only. Clients cannot choose their roles.
     * Username and e-mail are stored in lower case; the configured admin username cannot be registered.
     *
     * @throws ResourceAlreadyExistsException if the username or e-mail is already taken; the message does not
     *     say which
     */
    @Transactional
    public void register(RegisterRequest request) {
        String username = User.normalize(request.username());
        String email = User.normalize(request.email());
        // one message for both cases, so the response does not say which of the two is in use
        if (admin.isReserved(username)
                || userRepository.existsByUsername(username)
                || userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException(ALREADY_IN_USE);
        }

        userRepository.save(new User(
                null,
                request.name(),
                username,
                passwordEncoder.encode(request.password()),
                email,
                Set.of(roleRepository.getByName(RoleName.ROLE_USER))));
    }
}
