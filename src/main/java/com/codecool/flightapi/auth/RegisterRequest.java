package com.codecool.flightapi.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Self-registration payload. Deliberately has no roles: every self-registered account is a plain
 * user. Admin accounts are provisioned by {@link com.codecool.flightapi.user.AdminAccountInitializer}.
 *
 * @param password 8 to 72 characters (72 bytes is the BCrypt input limit)
 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+", message = "may only contain letters, digits, '.', '_' and '-'") String username,

        @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {}
