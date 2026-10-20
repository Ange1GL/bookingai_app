package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.PasswordRecoveryRequest;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PasswordRecoveryRequestResponse;

@Component
public class PasswordRecoveryRestMapper {

	public List<PasswordRecoveryRequestResponse> toResponses(List<PasswordRecoveryRequest> requests) {
		return requests.stream().map(this::toResponse).toList();
	}

	private PasswordRecoveryRequestResponse toResponse(PasswordRecoveryRequest request) {
		User user = request.user();
		return new PasswordRecoveryRequestResponse(request.id(), user.getId(), user.getUsername(), user.getName(),
				user.getLastName(), user.getPhone(), request.requestedAt());
	}
}
