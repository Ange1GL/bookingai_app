package com.github.angellariosacosta.bookingapp.application.result;

/**
 * Contenido de una notificación push.
 *
 * @param url relativa a la web: a dónde navega al tocar la notificación
 * @param tag una notificación con el mismo tag reemplaza a la anterior en vez de apilarse
 */
public record PushMessage(String title, String body, String url, String tag) {
}
