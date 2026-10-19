package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.CreateUserCommand;
import com.github.angellariosacosta.bookingapp.application.result.IssuedCredentialsResult;

/** Crea un usuario con contraseña temporal (debe cambiarla al entrar) y se la envía por WhatsApp. */
public interface CreateUserUseCase {

	IssuedCredentialsResult create(CreateUserCommand command);
}
