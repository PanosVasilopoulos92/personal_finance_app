package org.viators.personal_finance_app.dto.user.response;

import org.viators.personal_finance_app.model.User;

public record UserSummaryResponse(
        String uuid,
        String username,
        String fullName,
        String email
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getUsername(),
                user.getUuid(),
                user.getFirstName().concat(" ").concat(user.getLastName()),
                user.getEmail()
        );
    }
}
