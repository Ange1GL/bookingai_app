package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Propiedades {@code booking.blacklist.*}. {@code noShowThreshold} es el número de inasistencias
 * vigentes que manda a un cliente a la lista negra; 0 desactiva el auto-bloqueo.
 */
@Component
@ConfigurationProperties(prefix = "booking.blacklist")
@Validated
@Getter
@Setter
public class BlacklistProperties {

	@Min(0)
	private int noShowThreshold = 3;
}
