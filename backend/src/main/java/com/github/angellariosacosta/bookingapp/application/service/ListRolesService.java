package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ListRolesUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.query.ListRolesQuery;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListRolesService implements ListRolesUseCase {

	private final RoleRepository roleRepository;

	@Override
	@Transactional(readOnly = true)
	public PageResult<Role> list(ListRolesQuery query) {
		return roleRepository.findPage(normalize(query));
	}

	// Un filtro en blanco equivale a no filtrar.
	private ListRolesQuery normalize(ListRolesQuery query) {
		String search = query.search() == null || query.search().isBlank() ? null : query.search().strip();
		return new ListRolesQuery(search, query.active(), query.page(), query.size(), query.sortBy(), query.direction());
	}
}
