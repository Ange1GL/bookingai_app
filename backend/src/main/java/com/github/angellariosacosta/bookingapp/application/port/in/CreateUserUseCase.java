package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.CreateUserCommand;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCodeResult;

/** Crea un usuario sin contraseña y le envía por WhatsApp un código de un solo uso para que la cree. */
public interface CreateUserUseCase {

	IssuedCodeResult create(CreateUserCommand command);
}
