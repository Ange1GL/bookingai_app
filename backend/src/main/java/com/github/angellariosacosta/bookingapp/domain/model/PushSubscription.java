package com.github.angellariosacosta.bookingapp.domain.model;

/**
 * Dispositivo del usuario suscrito a notificaciones push. {@code p256dh} y {@code auth} son secretos del
 * dispositivo (cifran el mensaje): nunca se registran en logs.
 */
public record PushSubscription(Long id, Long userId, String endpoint, String p256dh, String auth, String userAgent) {
}
