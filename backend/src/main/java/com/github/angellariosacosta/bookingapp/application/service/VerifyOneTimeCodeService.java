package com.github.angellariosacosta.bookingapp.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.command.VerifyCodeCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.VerifyOneTimeCodeUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.VerificationTicketPort;
import com.github.angellariosacosta.bookingapp.application.result.VerifiedCodeResult;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidOneTimeCodeException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerifyOneTimeCodeService implements VerifyOneTimeCodeUseCase {

	static final Duration TICKET_TTL = Duration.ofMinutes(10);

	private final OneTimeCodeChecker checker;
	private final VerificationTicketPort tickets;
	private final Clock clock;

	// noRollbackFor: igual que el canje, un código incorrecto debe conservar el intento fallido que ya contó.
	@Override
	@Transactional(noRollbackFor = InvalidOneTimeCodeException.class)
	public VerifiedCodeResult verify(VerifyCodeCommand command) {
		Instant now = Instant.now(clock);
		OneTimeCodeChecker.Verified verified = checker.check(command.username(), command.code(), now);

		Instant ticketExpiry = now.plus(TICKET_TTL);
		Instant codeExpiry = verified.code().getExpiresAt();
		Instant expiresAt = ticketExpiry.isBefore(codeExpiry) ? ticketExpiry : codeExpiry;
		String ticket = tickets.issue(verified.user().getId(), verified.code().getId(), expiresAt);
		return new VerifiedCodeResult(ticket, expiresAt);
	}
}
