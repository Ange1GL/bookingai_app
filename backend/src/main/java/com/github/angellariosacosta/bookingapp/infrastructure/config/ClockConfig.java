package com.github.angellariosacosta.bookingapp.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

/**
 * Única fuente de "ahora" para la lógica de citas, fijada en la zona horaria del negocio.
 * Nadie debe llamar {@code LocalDateTime.now()} sin pasar este {@link Clock}: usaría la zona de la
 * JVM (UTC en la mayoría de servidores) y desfasaría las validaciones.
 */
@Configuration
@RequiredArgsConstructor
public class ClockConfig {

	private final BusinessTimeProperties businessTimeProperties;

	@Bean
	public Clock businessClock() {
		return Clock.system(businessTimeProperties.getTimezone());
	}
}
