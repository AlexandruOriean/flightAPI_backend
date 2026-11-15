package com.codecool.flightapi.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

/**
 * Body of {@code POST /api/v1/auth/password}.
 *
 * @param currentPassword checked against the stored hash
 * @param newPassword the same rules as at registration: 8 to 72 characters and at most 72 bytes in UTF-8
 */
public record ChangePasswordRequest(
        @NotBlank @Size(max = 255) String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {

    private static final int BCRYPT_MAX_BYTES = 72;

    @AssertTrue(message = "must be at most 72 bytes in UTF-8") public boolean isNewPasswordWithinByteLimit() {
        return newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }

    @AssertTrue(message = "must differ from the current password") public boolean isNewPasswordDifferent() {
        return newPassword == null || !newPassword.equals(currentPassword);
    }
}
