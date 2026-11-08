package com.github.angellariosacosta.bookingapp.application.port.in;

public interface RevokeSessionByUserIdUseCase {

    /** Cierra todas las sesiones del usuario: no puede renovar (refresh) ni seguir usando sus access tokens vivos. */
    void revokeSessionByUserId(Long actorId, Long userId);
}
