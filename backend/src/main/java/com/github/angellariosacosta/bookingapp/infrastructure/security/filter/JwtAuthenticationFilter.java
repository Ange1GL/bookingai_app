package com.github.angellariosacosta.bookingapp.infrastructure.security.filter;

import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.infrastructure.config.CookieProperties;
import com.github.angellariosacosta.bookingapp.infrastructure.security.entrypoint.SecurityEntryPoint;
import com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion.JwtAuthenticationException;
import com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion.JwtErrorCode;
import com.github.angellariosacosta.bookingapp.infrastructure.security.util.CookieUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;
    private final CookieProperties cookieProperties;
    private final SecurityEntryPoint securityEntryPoint;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Optional<String> tokenOpt = extractToken(request);

        if (tokenOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            tokenService.validateToken(tokenOpt.get());

            String email = tokenService.getEmail(tokenOpt.get());
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated user={}", email);

            filterChain.doFilter(request, response);

        } catch (JwtAuthenticationException ex) {
            SecurityContextHolder.clearContext();
            log.warn("JWT authentication failed: {}", ex.getErrorCode());
            securityEntryPoint.commence(request, response, ex);

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.warn("Unexpected error during JWT validation", ex);
            securityEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(JwtErrorCode.TOKEN_INVALID.getMessage(), ex)
            );
        }
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION_HEADER);
        if (bearer != null && bearer.startsWith(BEARER_PREFIX)) {
            return Optional.of(bearer.substring(BEARER_PREFIX.length()));
        }

        return CookieUtils.readCookie(request, cookieProperties.getAccess().getName());
    }
}
