package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.angellariosacosta.bookingapp.application.command.UpdateUserCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePhoneException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.exception.UserNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

class UpdateUserServiceTest {

	private static final long ID = 3L;

	private UserRepository repository;
	private UpdateUserService service;

	@BeforeEach
	void setUp() {
		repository = mock(UserRepository.class);
		service = new UpdateUserService(new UserFinder(repository), repository);
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(repository.findById(ID)).thenReturn(Optional.of(
				User.builder().id(ID).username("owner").email("old@example.com").name("Old").phone("5500000000")
						.active(true).build()));
	}

	@Test
	void updatesNameEmailAndNormalizedPhone() {
		User updated = service.update(new UpdateUserCommand(ID, "  New  ", "L", " new@example.com ", "55 1111 2222"));

		assertEquals("New", updated.getName());
		assertEquals("new@example.com", updated.getEmail());
		assertEquals("5511112222", updated.getPhone());
		assertEquals("owner", updated.getUsername());
	}

	@Test
	void nullPhoneKeepsCurrentOne() {
		assertEquals("5500000000", service.update(new UpdateUserCommand(ID, "n", "L", "old@example.com", null)).getPhone());
	}

	@Test
	void blankNameBecomesNull() {
		assertEquals(null, service.update(new UpdateUserCommand(ID, "  ", "L", "old@example.com", null)).getName());
	}

	@Test
	void rejectsEmailUsedByAnotherUser() {
		when(repository.existsByEmail("taken@example.com")).thenReturn(true);

		AuthException error = assertThrows(AuthException.class,
				() -> service.update(new UpdateUserCommand(ID, "n", "L", "taken@example.com", null)));

		assertEquals(AuthError.EMAIL_ALREADY_IN_USE, error.getError());
		verify(repository, never()).save(any());
	}

	@Test
	void keepingTheSameEmailSkipsUniquenessCheck() {
		service.update(new UpdateUserCommand(ID, "n", "L", "old@example.com", null));

		verify(repository, never()).existsByEmail(any());
	}

	@Test
	void rejectsPhoneUsedByAnotherUser() {
		when(repository.existsByPhone("5511112222")).thenReturn(true);

		assertThrows(DuplicatePhoneException.class,
				() -> service.update(new UpdateUserCommand(ID, "n", "L", "old@example.com", "5511112222")));
	}

	@Test
	void rejectsInvalidPhone() {
		assertThrows(InvalidFieldException.class, () -> service.update(new UpdateUserCommand(ID, "n", "L", "old@example.com", "abc")));
	}

	@Test
	void blankEmailClearsItWithoutCheckingUniqueness() {
		User updated = service.update(new UpdateUserCommand(ID, "n", "L", " ", null));

		assertEquals(null, updated.getEmail());
		verify(repository, never()).existsByEmail(any());
	}

	@Test
	void updatesLastNameAndKeepsTheTenDigitPhone() {
		User updated = service.update(new UpdateUserCommand(ID, "n", " Ruiz ", "old@example.com", "55 1111 2222"));

		assertEquals("Ruiz", updated.getLastName());
		assertEquals("5511112222", updated.getPhone());
	}

	@Test
	void reportsMissingUser() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> service.update(new UpdateUserCommand(99L, "n", "L", "a@b.co", null)));
	}
}
