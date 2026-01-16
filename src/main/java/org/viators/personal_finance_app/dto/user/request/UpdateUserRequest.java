package org.viators.personal_finance_app.dto.user.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record UpdateUserRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        @Min(value = 13, message = "Age must be at least 13")
        @Max(value = 141, message = "Age cannot exceed 141")
        Integer age,
        UserRolesEnum userRole
) {
    public UpdateUserRequest {
        if (username != null) {
            username = username.trim();
        }

        List<UserRolesEnum> availableRoles = Arrays.stream(UserRolesEnum.values()).toList();
        if (userRole != null && (!availableRoles.contains(userRole))) {
            throw new IllegalArgumentException("Not a valid role provided.");
        }
    }

    public void updateUser(User user) {
        Optional.ofNullable(username).ifPresent(user::setUsername);
        Optional.ofNullable(email).ifPresent(user::setEmail);
        Optional.ofNullable(firstName).ifPresent(user::setFirstName);
        Optional.ofNullable(lastName).ifPresent(user::setLastName);
        Optional.ofNullable(age).ifPresent(user::setAge);
        Optional.ofNullable(userRole).ifPresent(user::setUserRole);
    }
}
