package com.github.angellariosacosta.bookingapp.infrastructure.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.github.angellariosacosta.bookingapp.application.service.HandleCustomerMessageService;

/**
 * Procesa los mensajes de WhatsApp fuera del request del webhook para responder 200 a Meta de inmediato (si la IA
 * tarda, Meta reintentaría). Cola acotada: si se llena, el envío se rechaza y se registra en lugar de acumular sin límite.
 */
@Configuration
@EnableAsync
public class WhatsAppAsyncConfig {

	private static final int CORE_POOL_SIZE = 2;
	private static final int MAX_POOL_SIZE = 4;
	private static final int QUEUE_CAPACITY = 100;
	private static final String THREAD_NAME_PREFIX = "whatsapp-";

	@Bean(HandleCustomerMessageService.ASYNC_EXECUTOR)
	public Executor whatsappTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(CORE_POOL_SIZE);
		executor.setMaxPoolSize(MAX_POOL_SIZE);
		executor.setQueueCapacity(QUEUE_CAPACITY);
		executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
		return executor;
	}
}
