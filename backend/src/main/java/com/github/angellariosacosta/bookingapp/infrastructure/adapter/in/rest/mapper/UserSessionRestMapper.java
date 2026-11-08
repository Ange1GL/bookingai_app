package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.result.UserSessionResult;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.UserSessionResponse;

@Component
public class UserSessionRestMapper {

	public List<UserSessionResponse> toResponses(List<UserSessionResult> results) {
		return results.stream().map(this::toResponse).toList();
	}

	private UserSessionResponse toResponse(UserSessionResult result) {
		return new UserSessionResponse(
				result.id(), result.userAgent(), result.ipAddress(),
				result.startedAt(), result.lastActivityAt(), result.expiresAt());
	}
}
