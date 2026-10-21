package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.GetRoleUseCase;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetRoleService implements GetRoleUseCase {

	private final RoleFinder roleFinder;

	@Override
	@Transactional(readOnly = true)
	public Role get(Long id) {
		return roleFinder.getOrThrow(id);
	}
}
