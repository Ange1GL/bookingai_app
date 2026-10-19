package com.github.angellariosacosta.bookingapp.application.port.in;

/** Baja lógica de un permiso. Mismas protecciones que deshabilitar. */
public interface DeletePermissionUseCase {

	void delete(Long id);
}
