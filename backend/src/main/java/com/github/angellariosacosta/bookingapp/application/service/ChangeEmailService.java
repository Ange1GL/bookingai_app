package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.application.port.in.ChangeEmailUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeEmailService implements ChangeEmailUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void changeEmail(Long userId, String newEmail) {
        User user = userRepository.findById(userId)

                // TODO colocar una excepcion personalizada
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));

        if (!user.getEmail().equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new AuthException(AuthError.EMAIL_ALREADY_IN_USE);
        }

        user.setEmail(newEmail);
        userRepository.save(user);
    }
}
