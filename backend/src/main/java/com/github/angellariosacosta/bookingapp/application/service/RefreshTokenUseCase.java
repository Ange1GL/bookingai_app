package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion.JwtAuthenticationException;
import com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion.JwtErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public String refreshToken(String refreshToken) {

        if (!tokenService.validateToken(refreshToken)) {
            throw new JwtAuthenticationException(JwtErrorCode.TOKEN_INVALID);
        }

        Long userId = Long.parseLong(tokenService.getSubject(refreshToken));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JwtAuthenticationException(JwtErrorCode.TOKEN_INVALID));

        List<String> roles = user.getRoles().stream()
                .map(r -> r.name())
                .toList();

        return tokenService.generateToken(user.getId(), user.getEmail(), roles);
    }
}
