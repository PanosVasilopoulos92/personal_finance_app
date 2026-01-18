package org.viators.personal_finance_app.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.viators.personal_finance_app.security.UserDetailsImpl;

import java.util.Optional;

@Component("auditorAware")
public class SpringSecurityAuditorAware implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal() instanceof String) {
            return Optional.empty();
        }

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();

        return principal != null
                ? Optional.of(principal.getUsername())
                : Optional.empty();
    }
}
