package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.result.IssuedCodeResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UserOneTimeCodeResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.config.AdminUsersProperties;

class UserRestMapperOneTimeCodeTest {

	private static final String CODE = "483920";

	private final User withoutPassword = User.builder().id(5L).username("u").email("u@example.com")
			.phone("5215500000000").active(true).roles(Set.of(new Role(1L, "USER", null, true, true, Set.of()))).build();

	private UserRestMapper mapperWithExpose(boolean expose) {
		AdminUsersProperties properties = new AdminUsersProperties();
		properties.setExposeOneTimeCode(expose);
		return new UserRestMapper(properties);
	}

	@Test
	void neverExposesTheCodeByDefault() {
		UserOneTimeCodeResponse response = mapperWithExpose(false)
				.toOneTimeCodeResponse(new IssuedCodeResult(withoutPassword, false, CODE));

		assertNull(response.oneTimeCode());
		assertFalse(response.codeSent());
	}

	@Test
	void exposesTheCodeOnlyWhenTheFlagIsOnAndDeliveryFailed() {
		UserRestMapper mapper = mapperWithExpose(true);

		assertEquals(CODE, mapper.toOneTimeCodeResponse(new IssuedCodeResult(withoutPassword, false, CODE)).oneTimeCode());
		assertNull(mapper.toOneTimeCodeResponse(new IssuedCodeResult(withoutPassword, true, CODE)).oneTimeCode());
	}

	@Test
	void reportsWhetherThePasswordHasBeenCreatedWithoutLeakingTheHash() {
		UserRestMapper mapper = mapperWithExpose(false);
		User withPassword = User.builder().id(6L).username("v").email("v@example.com").password("$2b$secret-hash")
				.active(true).roles(Set.of()).build();

		assertFalse(mapper.toResponse(withoutPassword).passwordSet());
		assertTrue(mapper.toResponse(withPassword).passwordSet());
		assertFalse(mapper.toResponse(withPassword).toString().contains("secret-hash"));
	}
}
