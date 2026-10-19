package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.GetUserUseCase;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetUserService implements GetUserUseCase {

	private final UserFinder userFinder;

	@Override
	@Transactional(readOnly = true)
	public User get(Long id) {
		return userFinder.getOrThrow(id);
	}
}
