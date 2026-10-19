package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.result.IssuedCredentialsResult;

/** Regenera la contraseña temporal de un usuario, cierra sus sesiones y se la reenvía por WhatsApp. */
public interface ResetUserPasswordUseCase {

	IssuedCredentialsResult reset(Long actorId, Long userId);
}
