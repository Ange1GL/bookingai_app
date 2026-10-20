package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.AssignUserRolesCommand;
import com.github.angellariosacosta.bookingapp.application.command.CreateUserCommand;
import com.github.angellariosacosta.bookingapp.application.command.UpdateUserCommand;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCodeResult;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AssignUserRolesRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateUserRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PageResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.RoleSummaryResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UpdateUserRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UserOneTimeCodeResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UserResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.config.AdminUsersProperties;
import com.github.angellariosacosta.bookingapp.infrastructure.config.OtpDeliveryProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserRestMapper {

	private static final String SHARE_MESSAGE_FORMAT = "usuario: %s\ncodigoAcceso: %s";

	private final AdminUsersProperties adminUsersProperties;
	private final OtpDeliveryProperties otpDeliveryProperties;

	public CreateUserCommand toCommand(CreateUserRequest request) {
		return new CreateUserCommand(
				request.username(), request.email(), request.name(), request.lastName(), request.phone(), request.roleIds());
	}

	public UpdateUserCommand toCommand(Long id, UpdateUserRequest request) {
		return new UpdateUserCommand(id, request.name(), request.lastName(), request.email(), request.phone());
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
		return new UserResponse(user.getId(), user.getUsername(), user.getName(), user.getLastName(), user.getEmail(), user.getPhone(),
				user.isActive(), user.getPassword() != null, roles);
	}

	// El código solo se devuelve si WhatsApp no lo entregó y la entrega es manual (o estamos en dev). Con él viaja el
	// texto listo para pegar en el chat del cliente.
	public UserOneTimeCodeResponse toOneTimeCodeResponse(IssuedCodeResult result) {
		boolean expose = !result.delivered()
				&& (otpDeliveryProperties.isManual() || adminUsersProperties.isExposeOneTimeCode());
		String code = expose ? result.code() : null;
		String shareMessage = expose ? SHARE_MESSAGE_FORMAT.formatted(result.user().getUsername(), code) : null;
		return new UserOneTimeCodeResponse(toResponse(result.user()), result.delivered(), code, shareMessage);
	}

	public PageResponse<UserResponse> toPageResponse(PageResult<User> page) {
		return PageResponse.of(page.map(this::toResponse));
	}
}
