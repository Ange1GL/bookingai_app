package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.RegisterNoShowCommand;
import com.github.angellariosacosta.bookingapp.application.result.RegisterNoShowResult;

public interface RegisterNoShowUseCase {
	RegisterNoShowResult register(RegisterNoShowCommand command);
}
