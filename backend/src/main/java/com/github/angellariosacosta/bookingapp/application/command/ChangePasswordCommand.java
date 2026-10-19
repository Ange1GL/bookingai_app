package com.github.angellariosacosta.bookingapp.application.command;

/** {@code currentAccessToken} (puede ser null) es el JWT con el que se hizo la petición: se revoca al cambiar la clave. */
public record ChangePasswordCommand(
		Long userId,
		String currentPassword,
		String newPassword,
		String currentAccessToken,
		String userAgent,
		String ipAddress
) {

	@Override
	public String toString() {
		return "ChangePasswordCommand[userId=" + userId + "]";
	}
}
