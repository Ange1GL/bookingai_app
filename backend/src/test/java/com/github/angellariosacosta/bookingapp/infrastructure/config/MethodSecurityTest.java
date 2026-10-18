package com.github.angellariosacosta.bookingapp.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.angellariosacosta.bookingapp.infrastructure.security.Authorities;

/** Verifica que {@code hasAnyAuthority} con las constantes de {@link Authorities} se evalúa como se espera. */
class MethodSecurityTest {

	static class Guarded {
		@PreAuthorize("hasAnyAuthority('" + Authorities.USER_READ + "', '" + Authorities.ADMIN_ALL + "')")
		String read() {
			return "ok";
		}
	}

	@Configuration
	@EnableMethodSecurity
	static class Config {
		@Bean
		Guarded guarded() {
			return new Guarded();
		}
	}

	private static String callAs(String authority) {
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("u", "p", authority));
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
			return context.getBean(Guarded.class).read();
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void allowsRequiredAuthority() {
		assertEquals("ok", callAs(Authorities.USER_READ));
	}

	@Test
	void allowsAdminAll() {
		assertEquals("ok", callAs(Authorities.ADMIN_ALL));
	}

	@Test
	void deniesWithoutAuthority() {
		assertThrows(AccessDeniedException.class, () -> callAs("APPOINTMENT_READ"));
	}
}
