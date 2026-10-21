package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.UserNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class UserFinder {

	private final UserRepository userRepository;

	User getOrThrow(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + id));
	}
}
