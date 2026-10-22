package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Mismo formato que {@code PushSubscription.toJSON()} del navegador (los campos extra, como expirationTime, se ignoran). */
public record PushSubscriptionRequest(
		@NotBlank @Size(max = 2048) String endpoint,
		@NotNull @Valid Keys keys) {

	public record Keys(
			@NotBlank @Size(max = 255) String p256dh,
			@NotBlank @Size(max = 255) String auth) {
	}
}
