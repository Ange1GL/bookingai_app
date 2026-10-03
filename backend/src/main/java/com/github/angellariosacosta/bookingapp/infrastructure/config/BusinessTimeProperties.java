package com.github.angellariosacosta.bookingapp.infrastructure.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Zona horaria del negocio (propiedad {@code app.timezone}). Las fechas de las citas son "hora de
 * pared" en esta zona: no se convierten al guardar ni al leer. Un valor inválido (ej. "Mexico/Foo")
 * hace fallar el arranque, en vez de descubrirlo cuando un recordatorio sale desfasado.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class BusinessTimeProperties {

	@NotNull
	private ZoneId timezone;
}
