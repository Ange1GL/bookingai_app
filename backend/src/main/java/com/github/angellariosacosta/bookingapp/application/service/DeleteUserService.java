package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.DeleteUserUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RefreshTokenRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteUserService implements DeleteUserUseCase {

	private final UserFinder userFinder;
	private final UserAdminGuard adminGuard;
	private final UserRepository userRepository;
	private final RefreshTokenRepositoryPort refreshTokenRepository;

	@Override
	@Transactional
	public void delete(Long actorId, Long userId) {
		adminGuard.ensureNotSelf(actorId, userId, "No puedes eliminar tu propia cuenta");
		User user = userFinder.getOrThrow(userId);
		adminGuard.ensureAnotherActiveAdminExists(user);

		userRepository.markDeleted(user.getId());
		refreshTokenRepository.revokeAllByUserId(userId);
	}
}
