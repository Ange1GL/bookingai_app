package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.CreatePasswordCommand;
import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;

/** Canjea un código de un solo uso: define la contraseña de la cuenta y abre una sesión. Es un caso de uso público. */
public interface CreatePasswordUseCase {

	AuthTokenResult createPassword(CreatePasswordCommand command);
}
