package org.viators.personal_finance_app.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.viators.personal_finance_app.security.UserDetailsImpl;

@Component
public class UserSecurity {

    public boolean isSelf(String uuid) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return (userDetails.getUsername() != null && userDetails.getUsername().equals(uuid));
    }
}
