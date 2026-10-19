package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.ChangePasswordCommand;
import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;

/** Cambia la contraseña del usuario autenticado, cierra sus demás sesiones y le abre una nueva. */
public interface ChangePasswordUseCase {

	AuthTokenResult changePassword(ChangePasswordCommand command);
}
