package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.ListPasswordRecoveryRequestsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordRecoveryRequestRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.model.PasswordRecoveryRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListPasswordRecoveryRequestsService implements ListPasswordRecoveryRequestsUseCase {

	private final PasswordRecoveryRequestRepositoryPort requestRepository;

	@Override
	@Transactional(readOnly = true)
	public List<PasswordRecoveryRequest> listPending() {
		return requestRepository.findPending();
	}
}
