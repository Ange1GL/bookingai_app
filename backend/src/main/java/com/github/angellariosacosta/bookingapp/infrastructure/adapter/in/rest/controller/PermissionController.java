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

import com.github.angellariosacosta.bookingapp.application.port.in.ChangePermissionStatusUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.CreatePermissionUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.DeletePermissionUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.GetPermissionUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListPermissionsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.UpdatePermissionUseCase;
import com.github.angellariosacosta.bookingapp.application.query.ListPermissionsQuery;
import com.github.angellariosacosta.bookingapp.application.query.PermissionSortField;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreatePermissionRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PageResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PermissionResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UpdatePermissionRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.PermissionRestMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.security.Authorities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

	private static final String DEFAULT_PAGE = "0";
	private static final String DEFAULT_PAGE_SIZE = "20";
	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_SEARCH_LENGTH = 100;

	private final ListPermissionsUseCase listPermissions;
	private final GetPermissionUseCase getPermission;
	private final CreatePermissionUseCase createPermission;
	private final UpdatePermissionUseCase updatePermission;
	private final ChangePermissionStatusUseCase changePermissionStatus;
	private final DeletePermissionUseCase deletePermission;
	private final PermissionRestMapper mapper;

	@GetMapping
	@PreAuthorize(Authorities.CAN_READ_PERMISSIONS)
	public ResponseEntity<PageResponse<PermissionResponse>> list(
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
			@RequestParam(defaultValue = "NAME") PermissionSortField sortBy,
			@RequestParam(defaultValue = "ASC") SortDirection direction) {
		ListPermissionsQuery query = new ListPermissionsQuery(search, active, page, size, sortBy, direction);
		return ResponseEntity.ok(mapper.toPageResponse(listPermissions.list(query)));
	}

	@GetMapping("/{id}")
	@PreAuthorize(Authorities.CAN_READ_PERMISSIONS)
	public ResponseEntity<PermissionResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(mapper.toResponse(getPermission.get(id)));
	}

	@PostMapping
	@PreAuthorize(Authorities.CAN_WRITE_PERMISSIONS)
	public ResponseEntity<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest request) {
		PermissionResponse body = mapper.toResponse(createPermission.create(mapper.toCommand(request)));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Authorities.CAN_WRITE_PERMISSIONS)
	public ResponseEntity<PermissionResponse> update(
			@PathVariable Long id, @Valid @RequestBody UpdatePermissionRequest request) {
		return ResponseEntity.ok(mapper.toResponse(updatePermission.update(mapper.toCommand(id, request))));
	}

	@PatchMapping("/{id}/enable")
	@PreAuthorize(Authorities.CAN_WRITE_PERMISSIONS)
	public ResponseEntity<PermissionResponse> enable(@PathVariable Long id) {
		return ResponseEntity.ok(mapper.toResponse(changePermissionStatus.enable(id)));
	}

	@PatchMapping("/{id}/disable")
	@PreAuthorize(Authorities.CAN_WRITE_PERMISSIONS)
	public ResponseEntity<PermissionResponse> disable(@PathVariable Long id) {
		return ResponseEntity.ok(mapper.toResponse(changePermissionStatus.disable(id)));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize(Authorities.CAN_WRITE_PERMISSIONS)
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		deletePermission.delete(id);
		return ResponseEntity.noContent().build();
	}
}
