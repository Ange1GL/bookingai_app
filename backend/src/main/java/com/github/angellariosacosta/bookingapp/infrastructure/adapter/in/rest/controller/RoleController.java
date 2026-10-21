package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.port.in.AssignRolePermissionsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ChangeRoleStatusUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateRoleUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.DeleteRoleUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.GetRoleUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListRolesUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.UpdateRoleUseCase;
import com.github.angellariosacosta.bookingapp.application.query.ListRolesQuery;
import com.github.angellariosacosta.bookingapp.application.query.RoleSortField;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AssignRolePermissionsRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateRoleRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PageResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.RoleResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UpdateRoleRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.RoleRestMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.security.Authorities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

	private static final String DEFAULT_PAGE = "0";
	private static final String DEFAULT_PAGE_SIZE = "20";
	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_SEARCH_LENGTH = 100;

	private final ListRolesUseCase listRoles;
	private final GetRoleUseCase getRole;
	private final CreateRoleUseCase createRole;
	private final UpdateRoleUseCase updateRole;
	private final AssignRolePermissionsUseCase assignRolePermissions;
	private final ChangeRoleStatusUseCase changeRoleStatus;
	private final DeleteRoleUseCase deleteRole;
	private final RoleRestMapper mapper;

	@GetMapping
	@PreAuthorize(Authorities.CAN_READ_ROLES)
	public ResponseEntity<PageResponse<RoleResponse>> list(
			@RequestParam(name = "q", required = false)
			@Size(max = MAX_SEARCH_LENGTH, message = "El filtro q no puede superar los 100 caracteres")
			String search,
			@RequestParam(required = false) Boolean active,
			@RequestParam(defaultValue = DEFAULT_PAGE)
			@Min(value = 0, message = "page debe ser mayor o igual a 0")
			int page,
			@RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
			@Min(value = 1, message = "size debe ser al menos 1")
			@Max(value = MAX_PAGE_SIZE, message = "size no puede superar 50")
			int size,
			@RequestParam(defaultValue = "NAME") RoleSortField sortBy,
			@RequestParam(defaultValue = "ASC") SortDirection direction) {
		ListRolesQuery query = new ListRolesQuery(search, active, page, size, sortBy, direction);
		return ResponseEntity.ok(mapper.toPageResponse(listRoles.list(query)));
	}

	@GetMapping("/{id}")
	@PreAuthorize(Authorities.CAN_READ_ROLES)
	public ResponseEntity<RoleResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(mapper.toResponse(getRole.get(id)));
	}

	@PostMapping
	@PreAuthorize(Authorities.CAN_WRITE_ROLES)
	public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
		RoleResponse body = mapper.toResponse(createRole.create(mapper.toCommand(request)));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Authorities.CAN_WRITE_ROLES)
	public ResponseEntity<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
		return ResponseEntity.ok(mapper.toResponse(updateRole.update(mapper.toCommand(id, request))));
	}

	@PutMapping("/{id}/permissions")
	@PreAuthorize(Authorities.CAN_WRITE_ROLES)
	public ResponseEntity<RoleResponse> assignPermissions(
			@PathVariable Long id, @Valid @RequestBody AssignRolePermissionsRequest request) {
		return ResponseEntity.ok(mapper.toResponse(assignRolePermissions.assign(mapper.toCommand(id, request))));
	}

	@PatchMapping("/{id}/enable")
	@PreAuthorize(Authorities.CAN_WRITE_ROLES)
	public ResponseEntity<RoleResponse> enable(@PathVariable Long id) {
		return ResponseEntity.ok(mapper.toResponse(changeRoleStatus.enable(id)));
	}

	@PatchMapping("/{id}/disable")
	@PreAuthorize(Authorities.CAN_WRITE_ROLES)
	public ResponseEntity<RoleResponse> disable(@PathVariable Long id) {
		return ResponseEntity.ok(mapper.toResponse(changeRoleStatus.disable(id)));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize(Authorities.CAN_WRITE_ROLES)
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		deleteRole.delete(id);
		return ResponseEntity.noContent().build();
	}
}
