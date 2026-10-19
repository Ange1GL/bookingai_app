package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.result.IssuedCredentialsResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UserCredentialsResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.config.AdminUsersProperties;

class UserRestMapperCredentialsTest {

	private static final String TEMP = "Tmp-Pass#12";

	private final User user = User.builder().id(5L).username("u").email("u@example.com").phone("5215500000000")
			.password("hash").mustChangePassword(true).active(true)
			.roles(Set.of(new Role(1L, "USER", null, true, true, Set.of()))).build();

	private UserRestMapper mapperWithExpose(boolean expose) {
		AdminUsersProperties properties = new AdminUsersProperties();
		properties.setExposeTemporaryPassword(expose);
		return new UserRestMapper(properties);
	}

	@Test
	void neverExposesPasswordByDefault() {
		UserCredentialsResponse response = mapperWithExpose(false)
				.toCredentialsResponse(new IssuedCredentialsResult(user, false, TEMP));

		assertNull(response.temporaryPassword());
		assertFalse(response.credentialsSent());
	}

	@Test
	void exposesPasswordOnlyWhenFlagIsOnAndDeliveryFailed() {
		UserRestMapper mapper = mapperWithExpose(true);

		assertEquals(TEMP, mapper.toCredentialsResponse(new IssuedCredentialsResult(user, false, TEMP)).temporaryPassword());
		assertNull(mapper.toCredentialsResponse(new IssuedCredentialsResult(user, true, TEMP)).temporaryPassword());
	}

	@Test
	void responseNeverCarriesThePasswordHash() {
		UserCredentialsResponse response = mapperWithExpose(true)
				.toCredentialsResponse(new IssuedCredentialsResult(user, true, TEMP));

		assertTrue(response.credentialsSent());
		assertFalse(response.user().toString().contains("hash"));
		assertTrue(response.user().mustChangePassword());
	}
}
