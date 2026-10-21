package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.result.BootstrapAdminResult;

/** Garantiza que un usuario existente tenga el rol ADMIN; resuelve el primer administrador del sistema. */
public interface BootstrapAdminUseCase {

	BootstrapAdminResult ensureAdmin(String username);
}
