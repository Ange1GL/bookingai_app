package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AuthSuccessResponse;

@Component
public class AuthRestMapper {

	// Omite accessToken/refreshToken a propósito: los JWT viajan solo en cookies HttpOnly
	// (AuthController#writeAuthCookies). Exponerlos en el body anularía la mitigación XSS.
	public AuthSuccessResponse toResponse(AuthTokenResult result) {
		return new AuthSuccessResponse(result.userId(), result.username(), result.email(), result.roles());
	}
}
