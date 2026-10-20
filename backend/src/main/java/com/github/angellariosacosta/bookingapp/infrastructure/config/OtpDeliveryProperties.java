package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Cómo se entrega el código de un solo uso ({@code security.otp.delivery}). {@code MANUAL} (por defecto): el sistema
 * no envía nada y el código se devuelve al administrador para que se lo pase al usuario por su propio chat.
 * {@code WHATSAPP}: se envía con la plantilla de autenticación de Meta (requiere el negocio verificado).
 */
@Component
@ConfigurationProperties(prefix = "security.otp")
@Getter
@Setter
public class OtpDeliveryProperties {

	public enum Mode {
		MANUAL, WHATSAPP
	}

	private Mode delivery = Mode.MANUAL;

	public boolean isManual() {
		return delivery == Mode.MANUAL;
	}
}
