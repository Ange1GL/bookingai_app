package com.github.angellariosacosta.bookingapp.application.port.in;

/**
 * Caso de uso público: quien olvidó su contraseña indica su username y queda una solicitud para el administrador.
 * No informa si el usuario existe.
 */
public interface RequestPasswordRecoveryUseCase {

	void request(String username);
}
