package org.viators.personalfinanceapp.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.viators.personalfinanceapp.security.UserDetailsImpl;

@Component(value = "userSecurity")
public class UserSecurity {

    public boolean isSelf(String uuid) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        if (userDetails == null) return false;
        return userDetails.getUsername().equals(uuid);
    }
}
