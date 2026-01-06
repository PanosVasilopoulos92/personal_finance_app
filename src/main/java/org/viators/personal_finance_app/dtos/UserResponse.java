package org.viators.personal_finance_app.dtos;

import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;

import java.time.LocalDateTime;

public record UserResponse(
        String username,
        String uuid,
        String email,
        String fullName,
        UserRolesEnum userRole,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUuid(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getUserRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static UserResponse summary(User user) {
        return new UserResponse(
                user.getUsername(),
                user.getUuid(),
                null,
                user.getFullName(),
                null,
                user.getStatus(),
                null,
                null
        );
    }


}
