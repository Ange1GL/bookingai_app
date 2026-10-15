package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.command.CreateCompanyCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateCompanyUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.FindCompanyUseCase;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CompanyRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CompanyResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

	private final CreateCompanyUseCase createCompany;
	private final FindCompanyUseCase findCompany;

	// El dueño sale siempre del usuario autenticado, nunca del cuerpo de la petición.
	@PostMapping
	public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest request, @CurrentUserId Long userId) {
		CompanyResponse body = CompanyResponse.from(createCompany.create(new CreateCompanyCommand(request.name(), userId)));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/me")
	public ResponseEntity<CompanyResponse> me(@CurrentUserId Long userId) {
		return ResponseEntity.ok(CompanyResponse.from(findCompany.findByOwner(userId)));
	}
}
