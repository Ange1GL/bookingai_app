package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.CreatePermissionCommand;
import com.github.angellariosacosta.bookingapp.application.command.UpdatePermissionCommand;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreatePermissionRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PageResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PermissionResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UpdatePermissionRequest;

@Component
public class PermissionRestMapper {

	public CreatePermissionCommand toCommand(CreatePermissionRequest request) {
		return new CreatePermissionCommand(request.name(), request.description());
	}

	public UpdatePermissionCommand toCommand(Long id, UpdatePermissionRequest request) {
		return new UpdatePermissionCommand(id, request.description());
	}

	public PermissionResponse toResponse(Permission permission) {
		return new PermissionResponse(
				permission.id(), permission.name(), permission.description(), permission.active(), permission.system());
	}

	public PageResponse<PermissionResponse> toPageResponse(PageResult<Permission> page) {
		return PageResponse.of(page.map(this::toResponse));
	}
}
