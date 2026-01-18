package org.viators.personal_finance_app.dto.user.response;

import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;

public record UserAuthResponse(
        String token,
        String tokenType,
        String userUuid,
        String email,
        String username,
        UserRolesEnum userRole,
        Long expiresIn
) {
    public static UserAuthResponse of(String token, User user, long expiresIn) {
        return new UserAuthResponse(
                token,
                "Bearer",
                user.getUuid(),
                user.getEmail(),
                user.getUsername(),
                user.getUserRole(),
                expiresIn
        );
    }
}
