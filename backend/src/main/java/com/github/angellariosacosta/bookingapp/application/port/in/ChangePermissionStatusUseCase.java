package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.domain.model.Permission;

/** Habilita o deshabilita (lógicamente) un permiso. Deshabilitar exige que no esté en uso ni sea de sistema. */
public interface ChangePermissionStatusUseCase {

	Permission enable(Long id);

	Permission disable(Long id);
}
