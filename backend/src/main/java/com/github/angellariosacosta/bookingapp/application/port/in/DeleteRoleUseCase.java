package com.github.angellariosacosta.bookingapp.application.port.in;

/** Baja lógica de un rol. Mismas protecciones que deshabilitar. */
public interface DeleteRoleUseCase {

	void delete(Long id);
}
