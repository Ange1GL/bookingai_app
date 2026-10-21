package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.in.BootstrapAdminUseCase;
import com.github.angellariosacosta.bookingapp.application.result.BootstrapAdminResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Corre después de {@link DataInitializer}, que garantiza que el rol ADMIN exista. */
@Slf4j
@Component
@Order(BootstrapAdminInitializer.ORDER)
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {

	static final int ORDER = DataInitializer.ORDER + 1;

	private final BootstrapAdminProperties properties;
	private final BootstrapAdminUseCase bootstrapAdmin;

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.isConfigured()) {
			return;
		}
		String username = properties.getUsername().strip();
		BootstrapAdminResult result = bootstrapAdmin.ensureAdmin(username);
		switch (result) {
			case GRANTED -> log.info("Rol ADMIN asignado al usuario bootstrap '{}'", username);
			case ALREADY_ADMIN -> log.debug("El usuario bootstrap '{}' ya es ADMIN", username);
			case USER_NOT_FOUND -> log.warn("admin.bootstrap.username='{}' no existe; no se asigna ADMIN", username);
		}
	}
}
