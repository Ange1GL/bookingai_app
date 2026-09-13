package com.github.angellariosacosta.bookingapp.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.CurrentUserPort;
import com.github.angellariosacosta.bookingapp.infrastructure.config.CustomUserDetails;

@Component
public class CurrentUserAdapter implements CurrentUserPort {

	@Override
	public Long getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
			throw new IllegalStateException("No authenticated user found in the security context");
		}
		return principal.getUser().getId();
	}
}
