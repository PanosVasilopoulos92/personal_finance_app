package org.viators.personal_finance_app.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.viators.personal_finance_app.annotations.validators.PasswordMatch;

@PasswordMatch
public record UpdateUserPasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword,
        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {
    public UpdateUserPasswordRequest {
        if (currentPassword != null && newPassword != null && !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }
    }
}
