package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un parámetro {@code Long} de un método de controlador REST para que Spring lo resuelva
 * automáticamente con el id del usuario autenticado actual, en lugar de leerlo manualmente del
 * SecurityContext/JWT en cada endpoint.
 *
 * <p>La resuelve {@code CurrentUserIdArgumentResolver}, registrado en {@code WebConfig}, que
 * delega en {@code CurrentUserPort} para obtener el id del usuario autenticado.
 *
 * <p>Uso: {@code metodo(@RequestBody Request request, @CurrentUserId Long userId)}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
