package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.VerifyCodeCommand;
import com.github.angellariosacosta.bookingapp.application.result.VerifiedCodeResult;

/** Valida usuario + código sin consumirlo y entrega un comprobante de corta vida. Es un caso de uso público. */
public interface VerifyOneTimeCodeUseCase {

	VerifiedCodeResult verify(VerifyCodeCommand command);
}
