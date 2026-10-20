package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.port.in.ListPasswordRecoveryRequestsUseCase;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PasswordRecoveryRequestResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.PasswordRecoveryRestMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.security.Authorities;

import lombok.RequiredArgsConstructor;

/** Bandeja del administrador: usuarios que pidieron recuperar su contraseña (se atienden con reset-password). */
@RestController
@RequestMapping("/api/v1/password-recovery-requests")
@RequiredArgsConstructor
public class PasswordRecoveryRequestController {

	private final ListPasswordRecoveryRequestsUseCase listPendingRequests;
	private final PasswordRecoveryRestMapper mapper;

	@GetMapping
	@PreAuthorize(Authorities.CAN_READ_USERS)
	public ResponseEntity<List<PasswordRecoveryRequestResponse>> listPending() {
		return ResponseEntity.ok(mapper.toResponses(listPendingRequests.listPending()));
	}
}
