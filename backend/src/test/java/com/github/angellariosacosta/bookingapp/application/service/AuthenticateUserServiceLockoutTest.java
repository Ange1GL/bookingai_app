package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AccountBlockedException;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

/**
 * Prueba el bloqueo por intentos fallidos con transacciones reales: NO es {@code @Transactional} a propósito, porque
 * dentro de una transacción de prueba el rollback que este test debe detectar quedaría enmascarado. Crea y borra su
 * propio usuario.
 */
@SpringBootTest
class AuthenticateUserServiceLockoutTest {

	private static final String PASSWORD = "Correct-pass-1";
	private static final String WRONG_PASSWORD = "wrong-pass-1";

	@Autowired
	private AuthenticateUserService authenticateUserService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private PasswordHasher passwordHasher;
	@Autowired
	private JdbcTemplate jdbc;

	private String username;

	@BeforeEach
	void createUser() {
		username = "zz_lockout_" + UUID.randomUUID().toString().substring(0, 8);
		Role userRole = roleRepository.findByName(Role.USER_NAME).orElseThrow();
		userRepository.save(User.builder()
				.username(username).email(username + "@example.test").password(passwordHasher.hash(PASSWORD))
				.active(true).roles(Set.of(userRole)).build());
	}

	@AfterEach
	void deleteUser() {
		Long userId = jdbc.queryForObject("select user_id from user_app where username = ?", Long.class, username);
		jdbc.update("delete from account_blocked where user_id = ?", userId);
		jdbc.update("delete from refresh_token where user_id = ?", userId);
		jdbc.update("delete from user_roles where user_id = ?", userId);
		jdbc.update("delete from user_app where user_id = ?", userId);
	}

	@Test
	void threeWrongPasswordsBlockTheAccountEvenWithTheCorrectOne() {
		for (int attempt = 1; attempt <= 3; attempt++) {
			AuthException error = assertThrows(AuthException.class,
					() -> authenticateUserService.authenticate(username, WRONG_PASSWORD, "agent", "127.0.0.1"));
			assertEquals(AuthError.INVALID_CREDENTIALS, error.getError());
		}

		AccountBlockedException blocked = assertThrows(AccountBlockedException.class,
				() -> authenticateUserService.authenticate(username, PASSWORD, "agent", "127.0.0.1"));
		assertTrue(blocked.getMessage().contains("bloqueada"));
	}

	@Test
	void aSuccessfulLoginBeforeTheLimitResetsTheCounter() {
		for (int attempt = 1; attempt <= 2; attempt++) {
			assertThrows(AuthException.class,
					() -> authenticateUserService.authenticate(username, WRONG_PASSWORD, "agent", "127.0.0.1"));
		}
		authenticateUserService.authenticate(username, PASSWORD, "agent", "127.0.0.1");

		for (int attempt = 1; attempt <= 2; attempt++) {
			assertThrows(AuthException.class,
					() -> authenticateUserService.authenticate(username, WRONG_PASSWORD, "agent", "127.0.0.1"));
		}
		// 2 + reinicio + 2: si el contador no se hubiera reiniciado ya estaría bloqueada
		authenticateUserService.authenticate(username, PASSWORD, "agent", "127.0.0.1");
	}
}
