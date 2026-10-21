package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ChangeUserStatusUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeUserStatusService implements ChangeUserStatusUseCase {

	private final UserFinder userFinder;
	private final UserAdminGuard adminGuard;
	private final UserRepository userRepository;
	private final RefreshTokenRepositoryPort refreshTokenRepository;

	@Override
	@Transactional
	public User enable(Long userId) {
		User user = userFinder.getOrThrow(userId);
		if (user.isActive()) {
			return user;
		}
		user.setActive(true);
		return userRepository.save(user);
	}

	@Override
	@Transactional
	public User disable(Long actorId, Long userId) {
		adminGuard.ensureNotSelf(actorId, userId, "No puedes deshabilitar tu propia cuenta");
		User user = userFinder.getOrThrow(userId);
		if (!user.isActive()) {
			return user;
		}
		adminGuard.ensureAnotherActiveAdminExists(user);

		user.setActive(false);
		User saved = userRepository.save(user);
		// El filtro JWT ya rechaza al usuario inactivo; esto además impide renovar la sesión con el refresh token.
		refreshTokenRepository.revokeAllByUserId(userId);
		return saved;
	}
}
