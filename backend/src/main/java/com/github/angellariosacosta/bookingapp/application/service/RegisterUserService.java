package com.github.angellariosacosta.bookingapp.application.service;

import java.util.List;
import java.util.Set;

import com.github.angellariosacosta.bookingapp.application.command.AuthTokenCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.RegisterUserCase;
import com.github.angellariosacosta.bookingapp.application.port.out.PasswordHasher;
import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserCase {

    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenService tokenService;

    @Override
    public AuthTokenCommand register(String email, String rawPassword, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(AuthError.EMAIL_ALREADY_IN_USE);
        }

        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> {
                    log.error("Default role USER not found in database");
                    return new IllegalStateException("Default role USER not found");
                });

        User user = User.builder()
                .email(email)
                .password(passwordHasher.hash(rawPassword))
                .name(name)
                .active(true)
                .roles(Set.of(defaultRole))
                .build();

        user = userRepository.save(user);
        return buildTokenResult(user);
    }

    private AuthTokenCommand buildTokenResult(User user) {
        List<String> roles = user.getRoles().stream().map(Role::name).toList();
        String token = tokenService.generateToken(user.getId(), user.getEmail(), roles);
        String refreshToken = tokenService.generateRefreshToken(user.getId());
        return new AuthTokenCommand(token, refreshToken);
    }
}
