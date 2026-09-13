package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.command.AuthTokenCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.AuthenticateUserUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.RegisterUserCase;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.LoginRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RegisterUserCase registerUserCase;

    @PostMapping("/login")
    public ResponseEntity<AuthTokenCommand> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticateUserUseCase.authenticate(request.email(), request.password()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthTokenCommand> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokenCommand token = registerUserCase.register(request.email(), request.password(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }
}
