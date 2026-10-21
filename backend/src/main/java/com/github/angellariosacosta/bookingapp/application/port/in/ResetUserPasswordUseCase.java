package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.result.IssuedCodeResult;

/**
 * Invalida la contraseña actual del usuario, cierra sus sesiones y le envía por WhatsApp un código de un solo uso
 * para que cree una nueva.
 */
public interface ResetUserPasswordUseCase {

	IssuedCodeResult reset(Long actorId, Long userId);
}
