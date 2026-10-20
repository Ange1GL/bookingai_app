package com.github.angellariosacosta.bookingapp.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.port.out.PasswordRecoveryRequestRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.model.User;

class RequestPasswordRecoveryServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");
	private static final long USER_ID = 7L;

	private UserRepository users;
	private PasswordRecoveryRequestRepositoryPort requests;
	private RequestPasswordRecoveryService service;

	@BeforeEach
	void setUp() {
		users = mock(UserRepository.class);
		requests = mock(PasswordRecoveryRequestRepositoryPort.class);
		service = new RequestPasswordRecoveryService(users, requests, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private User user(boolean active, String phone) {
		return User.builder().id(USER_ID).username("maria").phone(phone).active(active).build();
	}

	@Test
	void registersAPendingRequestForAnActiveUserWithPhone() {
		when(users.findByUsername("maria")).thenReturn(Optional.of(user(true, "525512345678")));

		service.request("  maria ");

		verify(requests).registerPending(USER_ID, NOW);
	}

	@Test
	void silentlyIgnoresUnknownDisabledOrPhonelessUsers() {
		when(users.findByUsername("ghost")).thenReturn(Optional.empty());
		when(users.findByUsername("off")).thenReturn(Optional.of(user(false, "525512345678")));
		when(users.findByUsername("nophone")).thenReturn(Optional.of(user(true, null)));

		service.request("ghost");
		service.request("off");
		service.request("nophone");

		verify(requests, never()).registerPending(any(), any());
	}

	@Test
	void ignoresBlankUsernames() {
		service.request(" ");
		service.request(null);

		verify(users, never()).findByUsername(any());
	}
}
