package com.github.angellariosacosta.bookingapp.infrastructure.security.filter;



import com.github.angellariosacosta.bookingapp.application.port.out.TokenService;
import com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion.JwtAuthenticationException;
import com.github.angellariosacosta.bookingapp.infrastructure.security.excepcion.JwtErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {



    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;

    /** Patrones públicos (whitelist) */
    private final List<String> publicEndpointPatterns;

    /** Nombre de la cookie que transporta el JWT */
    private final String authCookieName;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(
            TokenService tokenService,
            UserDetailsService userDetailsService,
            List<String> publicEndpointPatterns,
            String authCookieName
    ) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
        this.publicEndpointPatterns = publicEndpointPatterns;
        this.authCookieName = authCookieName;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return publicEndpointPatterns.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

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

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (JwtAuthenticationException ex) {
            SecurityContextHolder.clearContext();
            throw ex;

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            throw new JwtAuthenticationException(JwtErrorCode.TOKEN_INVALID);
        }
    }

    private Optional<String> extractToken(HttpServletRequest request) {

        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return Optional.of(bearer.substring(7));
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> authCookieName.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
        }

        return Optional.empty();
    }
}


