package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.AssignRolePermissionsCommand;
import com.github.angellariosacosta.bookingapp.application.command.CreateRoleCommand;
import com.github.angellariosacosta.bookingapp.application.command.UpdateRoleCommand;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AssignRolePermissionsRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateRoleRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PageResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PermissionResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.RoleResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UpdateRoleRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleRestMapper {

	private final PermissionRestMapper permissionMapper;

	public CreateRoleCommand toCommand(CreateRoleRequest request) {
		return new CreateRoleCommand(request.name(), request.description(), request.permissionIds());
	}

	public UpdateRoleCommand toCommand(Long id, UpdateRoleRequest request) {
		return new UpdateRoleCommand(id, request.name(), request.description());
	}

	public AssignRolePermissionsCommand toCommand(Long id, AssignRolePermissionsRequest request) {
		return new AssignRolePermissionsCommand(id, request.permissionIds());
	}

	public RoleResponse toResponse(Role role) {
		List<PermissionResponse> permissions = role.permissions().stream()
				.sorted(Comparator.comparing(Permission::name))
				.map(permissionMapper::toResponse)
				.toList();
		return new RoleResponse(role.id(), role.name(), role.description(), role.active(), role.system(), permissions);
	}

	public PageResponse<RoleResponse> toPageResponse(PageResult<Role> page) {
		return PageResponse.of(page.map(this::toResponse));
	}
}
