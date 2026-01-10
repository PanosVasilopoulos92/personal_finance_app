package org.viators.personal_finance_app.dtos;

import jakarta.validation.constraints.*;
import org.viators.personal_finance_app.annotations.validators.PasswordMatch;
import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@PasswordMatch
public final class UserDTOs {

    private UserDTOs() {
        throw new UnsupportedOperationException("Utility class");
    }

    public record CreateUserRequest(
            @NotBlank(message = "Username is required")
            @Size(max = 50, min = 3, message = "username length must be between 3-50 characters")
            String username,

            @NotBlank(message = "Email is required")
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
                    message = "Password must contain: digit, lowercase, uppercase, and special character"
            )
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
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPassword(password); // Service must encrypt this!
            user.setAge(age);
            user.setUserRole(UserRolesEnum.USER);

            return user;
        }
    }

    public record UpdateUserRequest(
            String username,
            String email,
            String firstName,
            String lastName,
            String password,
            String confirmPassword,
            @Min(value = 1, message = "Age must be at least 1")
            @Max(value = 141, message = "Age cannot exceed 141")
            Integer age,
            UserRolesEnum userRole
    ) {
        public UpdateUserRequest {
            if (username != null) {
                username = username.trim();
            }

            if ((password != null && confirmPassword != null) && !password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Password does not match confirmation password");
            }

            List<UserRolesEnum> availableRoles = Arrays.stream(UserRolesEnum.values()).toList();
            if (userRole != null && (!availableRoles.contains(userRole))) {
                throw new IllegalArgumentException("Not a valid role provided.");
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

    public record UserDetailsResponse(
            String uuid,
            String username,
            String fullName,
            String email,
            Boolean isActive,
            UserRolesEnum userRole,
            LocalDateTime createdAt,
            UserPreferenceDTOs.UserPreferencesSummary userPreferences,
            List<ItemDTOs.ItemSummary> items,
            List<CategoryDTOs.CategorySummary> categories,
            List<PriceAlertDTOs.PriceAlertSummary> priceAlerts,
            List<ShoppingListDTOs.ShoppingListSummary> shoppingLists,
            List<InflationReportDTOs.InflationReportSummary> inflationReports,
            List<BasketDTOs.BasketSummary> baskets
    ) {

        public static UserDetailsResponse from(User user) {
            return new UserDetailsResponse(
                    user.getUsername(),
                    user.getUuid(),
                    user.getFirstName().concat(" ").concat(user.getLastName()),
                    user.getEmail(),
                    user.getStatus().equals("1"),
                    user.getUserRole(),
                    user.getCreatedAt(),
                    UserPreferenceDTOs.UserPreferencesSummary.from(user.getUserPreferences()),
                    ItemDTOs.ItemSummary.listOfSummaries(user.getItems()),
                    CategoryDTOs.CategorySummary.listOfSummaries(user.getCategories()),
                    PriceAlertDTOs.PriceAlertSummary.listOfSummaries(user.getPriceAlerts()),
                    ShoppingListDTOs.ShoppingListSummary.listOfSummaries(user.getShoppingLists()),
                    InflationReportDTOs.InflationReportSummary.listOfSummaries(user.getInflationReports()),
                    BasketDTOs.BasketSummary.listOfSummaries(user.getBaskets())
            );
        }
    }

    public record LoginUserRequest(
            @NotBlank(message = "Email is required")
            @Email
            String email,
            @NotBlank(message = "Password is required")
            String password
    ) {}

}