package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.List;

public interface TokenService {
    String generateToken(Long subject, String email, List<String> roles);

    String getSubject(String token);

    String getEmail(String token);

    boolean validateToken(String token);
}
