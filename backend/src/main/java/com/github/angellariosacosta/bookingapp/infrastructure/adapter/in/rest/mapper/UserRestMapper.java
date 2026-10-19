package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.AssignUserRolesCommand;
import com.github.angellariosacosta.bookingapp.application.command.UpdateUserCommand;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AssignUserRolesRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PageResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.RoleSummaryResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UpdateUserRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UserResponse;

@Component
public class UserRestMapper {

	public UpdateUserCommand toCommand(Long id, UpdateUserRequest request) {
		return new UpdateUserCommand(id, request.name(), request.email(), request.phone());
	}

	public AssignUserRolesCommand toCommand(Long actorId, Long userId, AssignUserRolesRequest request) {
		return new AssignUserRolesCommand(actorId, userId, request.roleIds());
	}

	// Nunca incluye la contraseña (hash) ni datos de sesión.
	public UserResponse toResponse(User user) {
		List<RoleSummaryResponse> roles = user.getRoles().stream()
				.sorted(Comparator.comparing(Role::name))
				.map(role -> new RoleSummaryResponse(role.id(), role.name()))
				.toList();
		return new UserResponse(user.getId(), user.getUsername(), user.getName(), user.getEmail(), user.getPhone(),
				user.isActive(), user.isMustChangePassword(), roles);
	}

	public PageResponse<UserResponse> toPageResponse(PageResult<User> page) {
		return PageResponse.of(page.map(this::toResponse));
	}
}
