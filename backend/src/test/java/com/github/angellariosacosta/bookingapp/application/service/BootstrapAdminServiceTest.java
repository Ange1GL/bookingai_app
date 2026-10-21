package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.result.BootstrapAdminResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

class BootstrapAdminServiceTest {

	private static final String USERNAME = "owner";

	private final Role userRole = new Role(1L, Role.USER_NAME, "Standard user", true, true, Set.of());
	private final Role adminRole = new Role(2L, Role.ADMIN_NAME, "Administrator", true, true, Set.of());

	private UserRepository userRepository;
	private RoleRepository roleRepository;
	private BootstrapAdminService service;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		roleRepository = mock(RoleRepository.class);
		service = new BootstrapAdminService(userRepository, roleRepository);
	}

	@Test
	void grantsAdminKeepingExistingRoles() {
		User user = User.builder().id(10L).username(USERNAME).roles(Set.of(userRole)).build();
		when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
		when(roleRepository.findByName(Role.ADMIN_NAME)).thenReturn(Optional.of(adminRole));

		assertEquals(BootstrapAdminResult.GRANTED, service.ensureAdmin(USERNAME));

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertEquals(Set.of(userRole, adminRole), saved.getValue().getRoles());
	}

	@Test
	void doesNothingWhenAlreadyAdmin() {
		User user = User.builder().id(10L).username(USERNAME).roles(Set.of(adminRole)).build();
		when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

		assertEquals(BootstrapAdminResult.ALREADY_ADMIN, service.ensureAdmin(USERNAME));

		verify(userRepository, never()).save(any());
	}

	@Test
	void reportsMissingUser() {
		when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

		assertEquals(BootstrapAdminResult.USER_NOT_FOUND, service.ensureAdmin(USERNAME));

		verify(userRepository, never()).save(any());
	}
}
