package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.result.AuthTokenResult;
import com.github.angellariosacosta.bookingapp.application.port.in.AuthenticateUserUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.LogoutUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.RefreshAccessTokenUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.RegisterUserCase;
import com.github.angellariosacosta.bookingapp.domain.exception.AuthException;
import com.github.angellariosacosta.bookingapp.domain.shared.AuthError;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AuthSuccessResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.LoginRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.RegisterRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.AuthRestMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.config.CookieProperties;
import com.github.angellariosacosta.bookingapp.infrastructure.security.util.AuthCookieFactory;
import com.github.angellariosacosta.bookingapp.infrastructure.security.util.CookieUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RegisterUserCase registerUserCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthCookieFactory authCookieFactory;
    private final CookieProperties cookieProperties;
    private final AuthRestMapper authRestMapper;

    @PostMapping("/login")
    public ResponseEntity<AuthSuccessResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthTokenResult result = authenticateUserUseCase.authenticate(
                request.username(), request.password(), httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr());
        writeAuthCookies(httpResponse, result);
        return ResponseEntity.ok(authRestMapper.toResponse(result));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthSuccessResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthTokenResult result = registerUserCase.register(
                request.username(), request.email(), request.password(), request.name(),
                httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr());
        writeAuthCookies(httpResponse, result);
        return ResponseEntity.status(HttpStatus.CREATED).body(authRestMapper.toResponse(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthSuccessResponse> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String rawRefreshToken = CookieUtils.readCookie(httpRequest, cookieProperties.getRefresh().getName())
                .orElseThrow(() -> new AuthException(AuthError.REFRESH_TOKEN_INVALID));
        AuthTokenResult result = refreshAccessTokenUseCase.refresh(
                rawRefreshToken, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr());
        writeAuthCookies(httpResponse, result);
        return ResponseEntity.ok(authRestMapper.toResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        CookieUtils.readCookie(httpRequest, cookieProperties.getRefresh().getName())
                .ifPresent(logoutUseCase::logout);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.expireAccessCookie().toString());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.expireRefreshCookie().toString());
        return ResponseEntity.noContent().build();
    }

    private void writeAuthCookies(HttpServletResponse response, AuthTokenResult result) {
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.buildAccessCookie(result.accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.buildRefreshCookie(result.refreshToken()).toString());
    }
}
