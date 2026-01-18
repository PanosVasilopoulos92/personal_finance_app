package org.viators.personal_finance_app.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Not a valid format for email")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}