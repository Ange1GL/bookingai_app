package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.UpdateUserCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.UpdateUserUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePhoneException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneNumbers;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateUserService implements UpdateUserUseCase {

	private final UserFinder userFinder;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public User update(UpdateUserCommand command) {
		User user = userFinder.getOrThrow(command.id());

		applyEmail(user, command.email());
		applyPhone(user, command.phone());
		user.setName(blankToNull(command.name()));

		return userRepository.save(user);
	}

	private void applyEmail(User user, String rawEmail) {
		if (rawEmail == null || rawEmail.isBlank()) {
			throw new InvalidFieldException("email", "must not be null or blank");
		}
		String email = rawEmail.strip();
		if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
			throw new AuthException(AuthError.EMAIL_ALREADY_IN_USE);
		}
		user.setEmail(email);
	}

	private void applyPhone(User user, String rawPhone) {
		if (rawPhone == null) {
			return;
		}
		String phone = PhoneNumbers.normalize(rawPhone);
		if (!phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
			throw new DuplicatePhoneException("Ya existe un usuario con ese teléfono");
		}
		user.setPhone(phone);
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
