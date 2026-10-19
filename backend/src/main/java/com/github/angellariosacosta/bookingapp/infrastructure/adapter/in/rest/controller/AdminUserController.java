package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.port.in.AssignUserRolesUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ChangeUserStatusUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.DeleteUserUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.GetUserUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListUsersUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.UpdateUserUseCase;
import com.github.angellariosacosta.bookingapp.application.query.ListUsersQuery;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.query.UserSortField;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AssignUserRolesRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PageResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UpdateUserRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UserResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.security.Authorities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private static final String DEFAULT_PAGE = "0";
	private static final String DEFAULT_PAGE_SIZE = "20";
	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_SEARCH_LENGTH = 100;

	private final ListUsersUseCase listUsers;
	private final GetUserUseCase getUser;
	private final UpdateUserUseCase updateUser;
	private final AssignUserRolesUseCase assignUserRoles;
	private final ChangeUserStatusUseCase changeUserStatus;
	private final DeleteUserUseCase deleteUser;
	private final UserRestMapper mapper;

	@GetMapping
	@PreAuthorize(Authorities.CAN_READ_USERS)
	public ResponseEntity<PageResponse<UserResponse>> list(
			@RequestParam(name = "q", required = false)
			@Size(max = MAX_SEARCH_LENGTH, message = "El filtro q no puede superar los 100 caracteres")
			String search,
			@RequestParam(required = false) Boolean active,
			@RequestParam(required = false) Long roleId,
			@RequestParam(defaultValue = DEFAULT_PAGE)
			@Min(value = 0, message = "page debe ser mayor o igual a 0")
			int page,
			@RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
			@Min(value = 1, message = "size debe ser al menos 1")
			@Max(value = MAX_PAGE_SIZE, message = "size no puede superar 50")
			int size,
			@RequestParam(defaultValue = "USERNAME") UserSortField sortBy,
			@RequestParam(defaultValue = "ASC") SortDirection direction) {
		ListUsersQuery query = new ListUsersQuery(search, active, roleId, page, size, sortBy, direction);
		return ResponseEntity.ok(mapper.toPageResponse(listUsers.list(query)));
	}

	@GetMapping("/{id}")
	@PreAuthorize(Authorities.CAN_READ_USERS)
	public ResponseEntity<UserResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(mapper.toResponse(getUser.get(id)));
	}

	@PutMapping("/{id}")
	@PreAuthorize(Authorities.CAN_WRITE_USERS)
	public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
		return ResponseEntity.ok(mapper.toResponse(updateUser.update(mapper.toCommand(id, request))));
	}

	@PutMapping("/{id}/roles")
	@PreAuthorize(Authorities.CAN_WRITE_USERS)
	public ResponseEntity<UserResponse> assignRoles(
			@PathVariable Long id, @Valid @RequestBody AssignUserRolesRequest request, @CurrentUserId Long actorId) {
		return ResponseEntity.ok(mapper.toResponse(assignUserRoles.assign(mapper.toCommand(actorId, id, request))));
	}

	@PatchMapping("/{id}/enable")
	@PreAuthorize(Authorities.CAN_WRITE_USERS)
	public ResponseEntity<UserResponse> enable(@PathVariable Long id) {
		return ResponseEntity.ok(mapper.toResponse(changeUserStatus.enable(id)));
	}

	@PatchMapping("/{id}/disable")
	@PreAuthorize(Authorities.CAN_WRITE_USERS)
	public ResponseEntity<UserResponse> disable(@PathVariable Long id, @CurrentUserId Long actorId) {
		return ResponseEntity.ok(mapper.toResponse(changeUserStatus.disable(actorId, id)));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize(Authorities.CAN_DELETE_USERS)
	public ResponseEntity<Void> delete(@PathVariable Long id, @CurrentUserId Long actorId) {
		deleteUser.delete(actorId, id);
		return ResponseEntity.noContent().build();
	}
}
