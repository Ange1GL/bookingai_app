package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.Instant;

public record VerifyCodeResponse(String verificationToken, Instant expiresAt) {

	@Override
	public String toString() {
		return "VerifyCodeResponse[expiresAt=" + expiresAt + "]";
	}
}
