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

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final LoadUserByUsernamePort loadUserByUsernamePort;
    private final PasswordHasher passwordEncoder;
    private final AuthTokenIssuer authTokenIssuer;
    private final AccountBlockedRepository accountBlockedRepository;

    @Override
    public AuthTokenResult authenticate(String username, String rawPassword, String userAgent, String ipAddress) {
        log.info("Login attempt user={}", username);

        User user = loadUserByUsernamePort.loadByUsername(username);

        Optional<AccountBlocked> accountBlocked = accountBlockedRepository.findByUserId(user.getId());

        accountBlocked.ifPresent(AccountBlocked::isBlocked);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("Login failed user={} reason=invalid_credentials", username);
            manageAccountLocked(accountBlocked, user.getId());
            throw new AuthException(AuthError.INVALID_CREDENTIALS);
        }

        if (!user.isActive()) {
            log.warn("Login failed user={} reason=user_disabled", username);
            throw new AuthException(AuthError.USER_DISABLED);
        }

        accountBlocked.ifPresent(accountBlockedRepository::resetAttempts);

        log.info("Login success user={}", username);
        return authTokenIssuer.issue(user, userAgent, ipAddress).result();
    }


    private  void manageAccountLocked(Optional<AccountBlocked> accountBlocked, Long userId) {
        accountBlocked.ifPresentOrElse(
                blocked -> {
                    blocked.incrementNumberOfAttempts();
                    accountBlockedRepository.updateNumberOfAttempts(blocked);
                    if (blocked.getEndExpiratedAt() != null) {
                        accountBlockedRepository.updateExpiratedAt(blocked);
                    }
                },
                () -> {
                    AccountBlocked newBlocked = AccountBlocked.create(userId);
                    accountBlockedRepository.save(newBlocked);
                }
        );
    }
}
