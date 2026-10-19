package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.AccessTokenRevocationRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.application.result.DecodedToken;
import com.github.angellariosacosta.bookingapp.domain.model.RevokedAccessToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Revoca un access token por su jti para que deje de valer antes de expirar. */
@Slf4j
@Component
@RequiredArgsConstructor
class AccessTokenRevoker {

	private final TokenService tokenService;
	private final AccessTokenRevocationRepositoryPort revocationRepository;

	// Best-effort: si el access token ya venció/es inválido, o si falla el guardado,
	// el flujo que lo invoca (logout, cambio de contraseña) debe completarse igual — nunca peor que el hueco
	// que esto viene a cerrar.
	void revoke(String rawAccessToken) {
		if (rawAccessToken == null || rawAccessToken.isBlank()) {
			return;
		}
		try {
			DecodedToken decoded = tokenService.decode(rawAccessToken);
			revocationRepository.revoke(RevokedAccessToken.builder()
					.jti(decoded.jti())
					.expiresAt(decoded.expiresAt())
					.revokedAt(Instant.now())
					.build());
		} catch (Exception ex) {
			log.warn("Access token revocation failed, continuing", ex);
		}
	}
}
