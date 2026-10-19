package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCode;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCodePurpose;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

/** Consultas reales contra la BD de pruebas (tabla creada por V25); cada test hace rollback. */
@SpringBootTest
@Transactional
class JpaOneTimeCodeRepositoryAdapterTest {

	@Autowired
	private JpaOneTimeCodeRepositoryAdapter codes;
	@Autowired
	private JpaUserRepositoryAdapter users;
	@Autowired
	private JpaRoleRepositoryAdapter roles;
	@Autowired
	private JdbcTemplate jdbc;

	private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

	private Long newUserId() {
		String username = "zz_otp_" + UUID.randomUUID().toString().substring(0, 8);
		return users.save(User.builder().username(username).email(username + "@example.test").active(true)
				.roles(Set.of(roles.findByName(Role.USER_NAME).orElseThrow())).build()).getId();
	}

	private OneTimeCode issue(Long userId, OneTimeCodePurpose purpose) {
		return codes.save(OneTimeCode.issue(userId, purpose, "hash", NOW, Duration.ofMinutes(30)));
	}

	@Test
	void theCatalogMatchesTheEnumOneToOne() {
		for (OneTimeCodePurpose purpose : OneTimeCodePurpose.values()) {
			String name = jdbc.queryForObject("select name from one_time_code_purpose where purpose_id = ?", String.class,
					purpose.getId());
			assertEquals(purpose.name(), name);
		}
		assertEquals(OneTimeCodePurpose.values().length,
				jdbc.queryForObject("select count(*) from one_time_code_purpose", Integer.class));
	}

	@Test
	void savesAndFindsThePendingCode() {
		Long userId = newUserId();

		OneTimeCode saved = issue(userId, OneTimeCodePurpose.PASSWORD_RESET);

		assertNotNull(saved.getId());
		OneTimeCode found = codes.findPendingByUserIdForUpdate(userId).orElseThrow();
		assertEquals(saved.getId(), found.getId());
		assertEquals(OneTimeCodePurpose.PASSWORD_RESET, found.getPurpose());
		assertEquals("hash", found.getCodeHash());
		assertEquals(NOW.plus(Duration.ofMinutes(30)), found.getExpiresAt());
		assertEquals(0, found.getFailedAttempts());
		assertNull(found.getConsumedAt());
	}

	@Test
	void updatesFailedAttemptsAndConsumption() {
		Long userId = newUserId();
		OneTimeCode code = issue(userId, OneTimeCodePurpose.ACCOUNT_ACTIVATION);

		code.registerFailure(NOW);
		code.registerFailure(NOW);
		codes.save(code);
		assertEquals(2, codes.findPendingByUserIdForUpdate(userId).orElseThrow().getFailedAttempts());

		code.consume(NOW.plusSeconds(1));
		codes.save(code);
		assertTrue(codes.findPendingByUserIdForUpdate(userId).isEmpty());
	}

	@Test
	void consumePendingInvalidatesTheOldCodeSoANewOneCanBeIssued() {
		Long userId = newUserId();
		OneTimeCode first = issue(userId, OneTimeCodePurpose.ACCOUNT_ACTIVATION);

		codes.consumePending(userId, NOW);
		OneTimeCode second = issue(userId, OneTimeCodePurpose.PASSWORD_RESET);

		assertEquals(second.getId(), codes.findPendingByUserIdForUpdate(userId).orElseThrow().getId());
		assertEquals(2, jdbc.queryForObject("select count(*) from one_time_code where user_id = ?", Integer.class, userId));
		assertNotNull(jdbc.queryForObject("select consumed_at from one_time_code where one_time_code_id = ?",
				java.sql.Timestamp.class, first.getId()));
	}

	@Test
	void theDatabaseRefusesTwoPendingCodesForTheSameUser() {
		Long userId = newUserId();
		issue(userId, OneTimeCodePurpose.ACCOUNT_ACTIVATION);

		org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
				() -> {
					issue(userId, OneTimeCodePurpose.PASSWORD_RESET);
					jdbc.execute("select 1");
				});
	}

	@Test
	void codesOfOtherUsersAreNotAffected() {
		Long first = newUserId();
		Long second = newUserId();
		issue(first, OneTimeCodePurpose.ACCOUNT_ACTIVATION);
		issue(second, OneTimeCodePurpose.ACCOUNT_ACTIVATION);

		codes.consumePending(first, NOW);

		assertTrue(codes.findPendingByUserIdForUpdate(first).isEmpty());
		assertTrue(codes.findPendingByUserIdForUpdate(second).isPresent());
	}
}
