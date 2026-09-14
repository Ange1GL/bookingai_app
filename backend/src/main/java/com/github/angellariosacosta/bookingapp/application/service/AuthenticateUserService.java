package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.application.command.AuthTokenCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.AuthenticateUserUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.LoadUserByEmailPort;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
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
    private final AuthTokenIssuer authTokenIssuer;

    @Override
    public AuthTokenCommand authenticate(String email, String rawPassword, String userAgent, String ipAddress) {
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
        return authTokenIssuer.issue(user, userAgent, ipAddress).command();
    }
}
