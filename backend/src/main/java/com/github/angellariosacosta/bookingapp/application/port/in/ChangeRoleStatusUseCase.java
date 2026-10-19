package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.domain.model.Role;

/** Habilita o deshabilita (lógicamente) un rol. Deshabilitar exige que no esté en uso ni sea de sistema. */
public interface ChangeRoleStatusUseCase {

	Role enable(Long id);

	Role disable(Long id);
}
