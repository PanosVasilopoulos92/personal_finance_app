package org.viators.personal_finance_app.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.model.UserPreferences;
import org.viators.personal_finance_app.model.enums.CurrencyEnum;
import org.viators.personal_finance_app.model.enums.ReportTypeEnum;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class UserDTOs {

    private UserDTOs() {
        throw new UnsupportedOperationException("Utility class");
    }

    public record CreateUserRequest(
            @NotBlank(message = "Username is required")
            @Size(max = 50, min = 3, message = "Username length must be between 3-50 characters")
            String username,

            @NotBlank(message = "Email is required")
            @Email(message = "Not a valid email address")
            String email,
            String firstName,
            String lastName,
            String password,
            String confirmPassword,
            Integer age
    ) {
        public CreateUserRequest {
            if (username != null) {
                username = username.trim();
            }

            if (age != null && age <= 0) {
                throw new IllegalArgumentException("Age cannot be zero or negative");
            }

            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Password does not match confirmation password");
            }
        }
    }

    public record UpdateUserRequest(
            @NotBlank(message = "Username is required")
            @Size(max = 50, min = 3, message = "Username length must be between 3-50 characters")
            String username,

            @NotBlank(message = "Email is required")
            @Email(message = "Not a valid email address")
            String email,
            String firstName,
            String lastName,
            String password,
            String confirmPassword,
            Integer age,
            UserRolesEnum userRole
    ) {
        public UpdateUserRequest {
            if (username != null) {
                username = username.trim();
            }

            if (age != null && age <= 0) {
                throw new IllegalArgumentException("Age cannot be zero or negative");
            }

            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Password does not match confirmation password");
            }
        }
    }

    public record UserSummary(
            String uuid,
            String username,
            String fullName,
            String email
    ) {
        public static UserSummary from(User user) {
            return new UserSummary(
                    user.getUsername(),
                    user.getUuid(),
                    user.getFirstName().concat(" ").concat(user.getLastName()),
                    user.getEmail()
            );
        }
    }

    public record UserDetailResponse(
            String uuid,
            String username,
            String fullName,
            String email,
            Boolean isActive,
            UserRolesEnum userRole,
            LocalDateTime createdAt,
            UserPreferencesSummary userPreferences
    ) {

        public record UserPreferencesSummary(
                CurrencyEnum defaultCurrency,
                String defaultLocation,
                Boolean notificationEnabled,
                Boolean emailAlerts,
                String preferredStoreIds
        ) {
            public static UserPreferencesSummary from(UserPreferences userPreferences) {
                return new UserPreferencesSummary(
                        userPreferences.getDefaultCurrency(),
                        userPreferences.getDefaultLocation(),
                        userPreferences.getNotificationEnabled(),
                        userPreferences.getEmailAlerts(),
                        userPreferences.getPreferredStoreIds()
                );
            }
        }

        public static UserDetailResponse from(User user) {
            return new UserDetailResponse(
                    user.getUsername(),
                    user.getUuid(),
                    user.getFirstName().concat(" ").concat(user.getLastName()),
                    user.getEmail(),
                    user.getStatus().equals("1"),
                    user.getUserRole(),
                    user.getCreatedAt(),
                    UserPreferencesSummary.from(user.getUserPreferences())
            );
        }
    }



}
