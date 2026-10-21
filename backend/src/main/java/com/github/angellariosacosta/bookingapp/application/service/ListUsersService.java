package com.github.angellariosacosta.bookingapp.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ListUsersUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.query.ListUsersQuery;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListUsersService implements ListUsersUseCase {

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public PageResult<User> list(ListUsersQuery query) {
		return userRepository.findPage(normalize(query));
	}

	// Un filtro en blanco equivale a no filtrar.
	private ListUsersQuery normalize(ListUsersQuery query) {
		String search = query.search() == null || query.search().isBlank() ? null : query.search().strip();
		return new ListUsersQuery(search, query.active(), query.roleId(), query.page(), query.size(),
				query.sortBy(), query.direction());
	}
}
