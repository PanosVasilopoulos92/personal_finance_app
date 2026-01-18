package org.viators.personal_finance_app.dto.user.request;

import jakarta.validation.constraints.*;
import org.viators.personal_finance_app.annotations.validators.PasswordMatch;
import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;

@PasswordMatch
public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 50, min = 3, message = "username length must be between 3-50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Size(max = 100, message = "Email cannot contain more than 100 characters")
        @Email(message = "not a valid email address")
        String email,

        @NotBlank(message = "First name is required")
        @Size(min = 3, max = 50, message = "first name can be between 3-50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 3, max = 50, message = "Last name can be between 3-50 characters")
        String lastName,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8-100 characters")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
                message = "Password must contain: digit, lowercase, uppercase, and special character")
        String password,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword,

        @Min(value = 1, message = "Age must be at least 1")
        @Max(value = 141, message = "Age cannot exceed 141")
        Integer age
) {
    // Compact Constructor
    public CreateUserRequest {
        if (username != null) {
            username = username.trim();
        }
    }

    public User toEntity() {
        return User.builder()
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password(password)
                .age(age)
                .build();
    }
}