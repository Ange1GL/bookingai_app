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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService implements TokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${security.jwt.expiration}")
    private long expirationMinutes;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Value("${security.jwt.audience}")
    private String audience;

    @Override
    public String generateToken(Long subject, String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(subject.toString())
                // jti (JWT ID) único por token: permite revocar UN access token puntual
                // (ver AccessTokenRevocationRepositoryPort) sin depender de nada más del
                // contenido del token, que nunca cambia entre dos tokens del mismo usuario
                // emitidos en el mismo segundo salvo por este id.
                .id(UUID.randomUUID().toString())
                .claim("user_id", subject)
                // username (no email): es el identificador estable de sesión. El email es un
                // dato de perfil editable y no debe quedar congelado dentro de un JWT ya emitido.
                // Los roles NO se embeben aquí: las authorities se recalculan desde BD en cada
                // request (CustomUserDetails.getAuthorities()), así que un claim "roles" en el
                // JWT nunca se lee y solo quedaría desactualizado si se revocan permisos.
                .claim(JwtClaims.USERNAME, username)
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
    public DecodedToken decode(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return new DecodedToken(jwt.getClaimAsString(JwtClaims.USERNAME), jwt.getId(), jwt.getExpiresAt());
        } catch (JwtValidationException ex) {
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
