package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.application.port.out.AccountBlockedRepository;
import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;
import com.github.angellariosacosta.bookingapp.application.port.in.AuthenticateUserUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.LoadUserByUsernamePort;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.model.AccountBlocked;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final LoadUserByUsernamePort loadUserByUsernamePort;
    private final PasswordHasher passwordEncoder;
    private final AuthTokenIssuer authTokenIssuer;
    private final AccountBlockedRepository accountBlockedRepository;
    private final FailedLoginAttempts failedLoginAttempts;

    // noRollbackFor: al fallar la contraseña se lanza AuthException tras contar el intento; con el rollback por defecto
    // ese conteo se deshacía y el bloqueo por intentos nunca se activaba (verificado por HTTP).
    @Transactional(noRollbackFor = AuthException.class)
    @Override
    public AuthTokenResult authenticate(String username, String rawPassword, String userAgent, String ipAddress) {
        log.info("Login attempt user={}", username);
        User user = loadUserByUsernamePort.loadByUsername(username);
        Optional<AccountBlocked> accountBlocked = accountBlockedRepository.findByUserId(user.getId());
        accountBlocked.ifPresent(AccountBlocked::ensureNotBlocked);
        if (!user.isActive()) {
            log.warn("Login failed user={} reason=user_disabled", username);
            throw new AuthException(AuthError.USER_DISABLED);
        }
        if (user.getPassword() == null) {
            // Cuenta pendiente de crear su contraseña con el código de un solo uso: no se puede iniciar sesión aún.
            log.warn("Login failed user={} reason=password_not_created", username);
            throw new AuthException(AuthError.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("Login failed user={} reason=invalid_credentials", username);
            failedLoginAttempts.register(user.getId());
            throw new AuthException(AuthError.INVALID_CREDENTIALS);
        }
        accountBlocked.ifPresent(accountBlockedRepository::resetAttempts);
        log.info("Login success user={}", username);
        return authTokenIssuer.issue(user, userAgent, ipAddress).result();
    }
}
