package com.github.angellariosacosta.bookingapp.application.port.in;

/** Baja lógica de un usuario. Mismas protecciones que deshabilitar. */
public interface DeleteUserUseCase {

	void delete(Long actorId, Long userId);
}
