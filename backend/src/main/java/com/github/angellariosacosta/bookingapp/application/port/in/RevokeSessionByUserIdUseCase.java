package com.github.angellariosacosta.bookingapp.application.port.in;

public interface RevokeSessionByUserIdUseCase {

    void revokeSessionByUserId(Long userId);
}
