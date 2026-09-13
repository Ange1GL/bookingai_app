package com.github.angellariosacosta.bookingapp.infrastructure.security.filter;


import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion.JwtAuthenticationException;
import com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion.JwtErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtTokenService implements TokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${security.jwt.expiration}")
    private long expirationMinutes;

    @Value("${security.jwt.refresh-expiration}")
    private long refreshExpirationMinutes;

    @Override
    public String generateToken(Long subject, String email, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject.toString())
                .claim("user_id", subject)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .build();

        JwtEncoderParameters params = JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims
        );

        return jwtEncoder.encode(params).getTokenValue();
    }

    @Override
    public String getEmail(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return jwt.getClaimAsString("email");
    }

    @Override
    public String generateRefreshToken(Long subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject.toString())
                .claim("user_id", subject)
                .issuedAt(now)
                .expiresAt(now.plus(refreshExpirationMinutes, ChronoUnit.MINUTES))
                .build();

        JwtEncoderParameters params = JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims
        );

        return jwtEncoder.encode(params).getTokenValue();
    }

    @Override
    public String getSubject(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return jwt.getSubject();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            jwtDecoder.decode(token);
            return true;
        } catch (JwtValidationException ex) {
            // 🔹 Puede contener múltiples errores
            if (ex.getErrors().stream()
                    .anyMatch(error ->
                            OAuth2ErrorCodes.INVALID_TOKEN.equals(error.getErrorCode())
                    )) {
                throw new JwtAuthenticationException(JwtErrorCode.TOKEN_EXPIRED);
            }
            throw new JwtAuthenticationException(JwtErrorCode.TOKEN_INVALID);

        } catch (BadJwtException ex) {
            throw new JwtAuthenticationException(JwtErrorCode.TOKEN_MALFORMED);

        } catch (JwtException ex) {
            throw new JwtAuthenticationException(JwtErrorCode.TOKEN_INVALID);
        }
    }


}
