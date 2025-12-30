package org.viators.personal_finance_app.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "username is required")
        @Size(max = 50, min = 3, message = "username length must be between 3-50 characters")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "not a valid email address")
        String email,
        String firstName,
        String lastName,
        String password,
        String confirmPassword,
        Integer age,
        String role
) {

}
