package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ListPermissionsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.query.ListPermissionsQuery;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListPermissionsService implements ListPermissionsUseCase {

	private final PermissionRepositoryPort permissionRepository;

	@Override
	@Transactional(readOnly = true)
	public PageResult<Permission> list(ListPermissionsQuery query) {
		return permissionRepository.findPage(normalize(query));
	}

	// Un filtro en blanco equivale a no filtrar.
	private ListPermissionsQuery normalize(ListPermissionsQuery query) {
		String search = query.search() == null || query.search().isBlank() ? null : query.search().strip();
		return new ListPermissionsQuery(
				search, query.active(), query.page(), query.size(), query.sortBy(), query.direction());
	}
}
