package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.github.angellariosacosta.bookingapp.application.command.CreatePasswordCommand;
import com.github.angellariosacosta.bookingapp.application.command.CreateUserCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCodeResult;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidOneTimeCodeException;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCode;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

/**
 * Flujo completo con servicios y BD reales: alta sin contraseña -> código -> crear contraseña -> login -> reinicio.
 * NO es {@code @Transactional} a propósito: el conteo de intentos fallidos debe sobrevivir al rollback de la excepción
 * y dentro de una transacción de prueba eso quedaría enmascarado. Crea y borra su propio usuario.
 */
@SpringBootTest
class CreatePasswordFlowTest {

	private static final String PASSWORD = "My-first-password-1";
	private static final String OTHER_PASSWORD = "My-second-password-2";
	private static final String WRONG_CODE = "000000";

	@Autowired
	private CreateUserService createUser;
	@Autowired
	private ResetUserPasswordService resetPassword;
	@Autowired
	private CreatePasswordService createPassword;
	@Autowired
	private AuthenticateUserService authenticate;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private JdbcTemplate jdbc;

	private String username;
	private IssuedCodeResult created;

	@BeforeEach
	void createUserWithoutPassword() {
		username = "zz_flow_" + UUID.randomUUID().toString().substring(0, 8);
		Role userRole = roleRepository.findByName(Role.USER_NAME).orElseThrow();
		String phone = "5299" + String.format("%08d", Math.abs(username.hashCode()) % 100_000_000);
		created = createUser.create(new CreateUserCommand(username, username + "@example.test", "Flow", phone,
				Set.of(userRole.id())));
	}

	@AfterEach
	void deleteUser() {
		Long userId = jdbc.queryForObject("select user_id from user_app where username = ?", Long.class, username);
		jdbc.update("delete from one_time_code where user_id = ?", userId);
		jdbc.update("delete from account_blocked where user_id = ?", userId);
		jdbc.update("delete from refresh_token where user_id = ?", userId);
		jdbc.update("delete from user_roles where user_id = ?", userId);
		jdbc.update("delete from user_app where user_id = ?", userId);
	}

	private AuthTokenResult redeem(String code, String password) {
		return createPassword.createPassword(new CreatePasswordCommand(username, code, password, "agent", "127.0.0.1"));
	}

	private AuthTokenResult login(String password) {
		return authenticate.authenticate(username, password, "agent", "127.0.0.1");
	}

	@Test
	void theNewAccountHasNoPasswordAndCannotLogInUntilItRedeemsTheCode() {
		assertNull(userRepository.findByUsername(username).orElseThrow().getPassword());
		assertFalse(created.delivered());

		AuthException error = assertThrows(AuthException.class, () -> login(PASSWORD));
		assertEquals(AuthError.INVALID_CREDENTIALS, error.getError());

		AuthTokenResult session = redeem(created.code(), PASSWORD);

		assertEquals(username, session.username());
		assertEquals(username, login(PASSWORD).username());
	}

	@Test
	void aCodeWorksOnlyOnce() {
		redeem(created.code(), PASSWORD);

		assertThrows(InvalidOneTimeCodeException.class, () -> redeem(created.code(), OTHER_PASSWORD));
		assertEquals(username, login(PASSWORD).username());
	}

	@Test
	void fiveWrongCodesBurnTheCodeEvenIfTheRightOneComesNext() {
		for (int attempt = 1; attempt <= OneTimeCode.MAX_FAILED_ATTEMPTS; attempt++) {
			assertThrows(InvalidOneTimeCodeException.class, () -> redeem(WRONG_CODE, PASSWORD));
		}

		assertThrows(InvalidOneTimeCodeException.class, () -> redeem(created.code(), PASSWORD));
		assertNull(userRepository.findByUsername(username).orElseThrow().getPassword());
	}

	@Test
	void aWeakPasswordDoesNotSpendTheCode() {
		assertThrows(RuntimeException.class, () -> redeem(created.code(), "short1"));

		redeem(created.code(), PASSWORD);
		assertEquals(username, login(PASSWORD).username());
	}

	@Test
	void resetInvalidatesTheCurrentPasswordAndTheNewCodeCreatesAnother() {
		redeem(created.code(), PASSWORD);
		Long adminLikeActor = -1L;
		Long userId = userRepository.findByUsername(username).orElseThrow().getId();

		IssuedCodeResult reset = resetPassword.reset(adminLikeActor, userId);

		assertNull(userRepository.findByUsername(username).orElseThrow().getPassword());
		assertThrows(AuthException.class, () -> login(PASSWORD));
		assertThrows(InvalidOneTimeCodeException.class, () -> redeem(created.code(), OTHER_PASSWORD));

		redeem(reset.code(), OTHER_PASSWORD);
		assertEquals(username, login(OTHER_PASSWORD).username());
		assertTrue(reset.code() != null && !reset.code().isBlank());
	}

	@Test
	void issuingANewCodeInvalidatesThePreviousOne() {
		Long userId = userRepository.findByUsername(username).orElseThrow().getId();

		IssuedCodeResult second = resetPassword.reset(-1L, userId);

		assertThrows(InvalidOneTimeCodeException.class, () -> redeem(created.code(), PASSWORD));
		redeem(second.code(), PASSWORD);
		assertEquals(username, login(PASSWORD).username());
	}
}
