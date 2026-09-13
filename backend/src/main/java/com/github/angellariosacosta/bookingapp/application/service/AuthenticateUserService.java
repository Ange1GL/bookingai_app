package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;

import com.github.angellariosacosta.bookingapp.application.command.AuthTokenCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.AuthenticateUserUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.LoadUserByEmailPort;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final LoadUserByEmailPort loadUserByEmailPort;
    private final PasswordHasher passwordEncoder;
    private final TokenService tokenService;

    @Override
    public AuthTokenCommand authenticate(String email, String rawPassword) {
        log.info("Login attempt user={}", email);

        User user = loadUserByEmailPort.loadByEmail(email);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("Login failed user={} reason=invalid_credentials", email);
            throw new AuthException(AuthError.INVALID_CREDENTIALS);
        }

        if (!user.isActive()) {
            log.warn("Login failed user={} reason=user_disabled", email);
            throw new AuthException(AuthError.USER_DISABLED);
        }

        log.info("Login success user={}", email);
        return buildTokenResult(user);
    }

    private AuthTokenCommand buildTokenResult(User user) {
        List<String> roles = user.getRoles().stream().map(Role::name).toList();
        String token = tokenService.generateToken(user.getId(), user.getEmail(), roles);
        String refreshToken = tokenService.generateRefreshToken(user.getId());
        return new AuthTokenCommand(token, refreshToken);
    }
}
