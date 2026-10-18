package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Primer administrador ({@code admin.bootstrap.*}). Si {@code username} no está vacío, ese usuario existente recibe el
 * rol ADMIN al arrancar (idempotente). Ver docs/admin-users-roles.md.
 */
@Component
@ConfigurationProperties(prefix = "admin.bootstrap")
@Getter
@Setter
public class BootstrapAdminProperties {

	private String username = "";

	public boolean isConfigured() {
		return username != null && !username.isBlank();
	}
}
