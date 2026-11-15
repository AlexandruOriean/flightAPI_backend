package com.codecool.flightapi.user;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Provisions the administrator account from {@link AdminAccountProperties} at startup. This is the
 * only way to obtain {@link RoleName#ROLE_ADMIN}: self-registration always creates plain users.
 *
 * <ul>
 *   <li>Not configured (no password): nothing happens.
 *   <li>User does not exist: it is created with ROLE_USER and ROLE_ADMIN.
 *   <li>User exists and is an admin: nothing happens. The password is never overwritten.
 *   <li>User exists without ROLE_ADMIN: startup fails. The account is never promoted, because its
 *       password is whatever its owner chose at registration.
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AdminAccountProperties admin;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // a misconfigured admin stops the startup before anything touches the database
        if (admin.isConfigured()) {
            validate();
        }
        warnAboutUnexpectedAdmins();
        if (!admin.isConfigured()) {
            log.info("No admin account configured (app.admin.password is empty); skipping provisioning");
            return;
        }

        userRepository
                .getByUsername(User.normalize(admin.username()))
                .ifPresentOrElse(this::requireAdminRole, this::createAdmin);
    }

    /**
     * Earlier versions let self-registration ask for {@code ROLE_ADMIN}. The only intended admin is
     * the configured one, so every other admin account in an existing database is reported at each startup until
     * someone has checked it. Nothing is changed automatically: the account may have been made admin on purpose.
     */
    private void warnAboutUnexpectedAdmins() {
        String configured = admin.isConfigured() ? User.normalize(admin.username()) : null;
        List<String> unexpected = userRepository.findUsernamesByRole(RoleName.ROLE_ADMIN).stream()
                .filter(username -> !username.equals(configured))
                .toList();
        if (!unexpected.isEmpty()) {
            log.warn(
                    "Accounts with ROLE_ADMIN other than the configured admin: {}. Earlier versions let self-registered"
                            + " accounts request ROLE_ADMIN; check each one and remove the role from any that should"
                            + " not have it (see README, \"Admin accounts\").",
                    unexpected);
        }
    }

    private void validate() {
        if (!StringUtils.hasText(admin.username()) || !StringUtils.hasText(admin.email())) {
            throw new IllegalStateException(
                    "app.admin.username and app.admin.email are required with app.admin.password");
        }
        if (admin.password().length() < AdminAccountProperties.MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("app.admin.password must be at least "
                    + AdminAccountProperties.MIN_PASSWORD_LENGTH + " characters");
        }
    }

    private void createAdmin() {
        userRepository.save(new User(
                null,
                admin.username(),
                admin.username(),
                passwordEncoder.encode(admin.password()),
                admin.email(),
                Set.of(roleRepository.getByName(RoleName.ROLE_USER), roleRepository.getByName(RoleName.ROLE_ADMIN))));
        log.info("Created admin account '{}'", admin.username());
    }

    private void requireAdminRole(User user) {
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            throw new IllegalStateException("app.admin.username '" + user.getUsername()
                    + "' belongs to an existing account without ROLE_ADMIN; refusing to promote it."
                    + " Choose another app.admin.username, or verify that account and grant the role in the database");
        }
        log.info("Admin account '{}' already exists", user.getUsername());
    }
}
