package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.domain.model.User;

/**
 * Habilita o deshabilita un usuario. Deshabilitar corta su acceso de inmediato (revoca sus refresh tokens) y
 * no puede aplicarse a uno mismo ni al último administrador activo.
 */
public interface ChangeUserStatusUseCase {

	User enable(Long userId);

	User disable(Long actorId, Long userId);
}
