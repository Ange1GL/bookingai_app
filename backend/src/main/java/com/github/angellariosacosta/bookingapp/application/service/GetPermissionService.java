package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.GetPermissionUseCase;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPermissionService implements GetPermissionUseCase {

	private final PermissionFinder permissionFinder;

	@Override
	@Transactional(readOnly = true)
	public Permission get(Long id) {
		return permissionFinder.getOrThrow(id);
	}
}
