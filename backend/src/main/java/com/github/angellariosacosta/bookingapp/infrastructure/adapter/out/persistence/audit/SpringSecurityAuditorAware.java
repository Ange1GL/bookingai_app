package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.audit;


import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();


        if (authentication == null) {
            return Optional.of("SYSTEM");
        }

        if (!authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }


        return Optional.of(authentication.getName());
    }
}
