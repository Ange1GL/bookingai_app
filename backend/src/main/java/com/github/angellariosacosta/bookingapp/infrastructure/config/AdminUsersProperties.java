package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Administración de usuarios ({@code admin.users.*}). {@code exposeTemporaryPassword} solo debe activarse en el perfil
 * dev: devuelve la contraseña temporal en la respuesta, y únicamente cuando WhatsApp no la pudo entregar.
 */
@Component
@ConfigurationProperties(prefix = "admin.users")
@Getter
@Setter
public class AdminUsersProperties {

	private boolean exposeTemporaryPassword = false;
}
