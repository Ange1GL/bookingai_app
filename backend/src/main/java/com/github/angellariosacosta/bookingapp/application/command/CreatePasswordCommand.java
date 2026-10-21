package com.github.angellariosacosta.bookingapp.application.command;

/** El usuario canjea el código recibido por WhatsApp y define su contraseña. */
public record CreatePasswordCommand(
		String username,
		String code,
		String newPassword,
		String verificationToken,
		String userAgent,
		String ipAddress
) {

	@Override
	public String toString() {
		return "CreatePasswordCommand[username=" + username + "]";
	}
}
