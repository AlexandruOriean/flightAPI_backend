package com.codecool.flightapi.user;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * The administrator account provisioned at startup, e.g. from the environment variables
 * {@code APP_ADMIN_USERNAME}, {@code APP_ADMIN_EMAIL} and {@code APP_ADMIN_PASSWORD}.
 * Provisioning is skipped when no password is configured.
 */
@ConfigurationProperties("app.admin")
public record AdminAccountProperties(String username, String email, String password) {

    /** Minimum length for the configured admin password. */
    public static final int MIN_PASSWORD_LENGTH = 12;

    public boolean isConfigured() {
        return StringUtils.hasText(password);
    }

    /**
     * Whether {@code username} is the admin's (ignoring case) and therefore closed to self-registration,
     * also while no admin is provisioned yet: whoever held that name would otherwise be taken for the admin.
     */
    public boolean isReserved(String username) {
        return this.username != null && this.username.equalsIgnoreCase(username);
    }

    @Override
    public String toString() {
        // never expose the password, e.g. in logs or actuator output
        return "AdminAccountProperties[username=" + username + ", email=" + email + ", password=****]";
    }
}
