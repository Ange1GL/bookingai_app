package com.github.angellariosacosta.bookingapp.application.service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.github.angellariosacosta.bookingapp.application.command.HandleCustomerMessageCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.HandleCustomerMessageUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerAssistantPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRateLimitPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerReplyPort;
import com.github.angellariosacosta.bookingapp.application.port.out.ProcessedMessagePort;
import com.github.angellariosacosta.bookingapp.application.result.RateLimitDecision;
import com.github.angellariosacosta.bookingapp.domain.shared.PhoneMask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Se ejecuta fuera del request del webhook (Meta ya recibió su 200). Orden de las defensas: remitente válido,
 * idempotencia por id de mensaje, límite por teléfono y, ya dentro, turnos de una misma conversación en serie.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandleCustomerMessageService implements HandleCustomerMessageUseCase {

	/** Nombre del executor definido en {@code WhatsAppAsyncConfig}. */
	public static final String ASYNC_EXECUTOR = "whatsappTaskExecutor";

	static final String UNSUPPORTED_MESSAGE_REPLY = "Por ahora solo puedo leer mensajes de texto. ¿Me escribes tu mensaje?";
	static final String ASSISTANT_UNAVAILABLE_REPLY = "Tuve un problema para procesar tu mensaje. Inténtalo de nuevo en unos minutos.";
	static final String RATE_LIMITED_REPLY = "Estás enviando mensajes muy rápido. Espera un minuto e inténtalo de nuevo.";

	private static final int LOCK_STRIPES = 64;

	private final CustomerAssistantPort assistant;
	private final CustomerReplyPort replyPort;
	private final ProcessedMessagePort processedMessages;
	private final CustomerRateLimitPort rateLimit;

	// Dos mensajes seguidos del mismo cliente no deben intercalar sus turnos (estado y memoria del chat compartidos).
	private final Lock[] conversationLocks = IntStream.range(0, LOCK_STRIPES)
			.mapToObj(index -> new ReentrantLock())
			.toArray(Lock[]::new);

	@Override
	@Async(ASYNC_EXECUTOR)
	public void handle(HandleCustomerMessageCommand command) {
		if (!command.hasSender()) {
			log.warn("Mensaje de WhatsApp sin remitente, se ignora");
			return;
		}
		if (isDuplicate(command)) {
			log.info("Mensaje de WhatsApp duplicado id={}, se ignora", command.messageId());
			return;
		}
		if (!passesRateLimit(command)) {
			return;
		}
		Lock lock = lockFor(command.conversationKey());
		lock.lock();
		try {
			respond(command);
		} finally {
			lock.unlock();
		}
	}

	private boolean isDuplicate(HandleCustomerMessageCommand command) {
		return command.hasMessageId() && !processedMessages.markIfNew(command.messageId());
	}

	private boolean passesRateLimit(HandleCustomerMessageCommand command) {
		RateLimitDecision decision = rateLimit.tryAcquire(command.from());
		return switch (decision) {
			case ALLOWED -> true;
			case REJECTED_NOTIFY -> {
				log.warn("Límite de mensajes excedido por {}", PhoneMask.mask(command.from()));
				replyPort.send(command.phoneNumberId(), command.from(), RATE_LIMITED_REPLY);
				yield false;
			}
			case REJECTED_SILENT -> false;
		};
	}

	private void respond(HandleCustomerMessageCommand command) {
		String reply = command.hasReadableText() ? askAssistant(command) : UNSUPPORTED_MESSAGE_REPLY;
		replyPort.send(command.phoneNumberId(), command.from(), reply);
	}

	private String askAssistant(HandleCustomerMessageCommand command) {
		try {
			return assistant.reply(command);
		} catch (RuntimeException ex) {
			log.error("Error del asistente al responder a un cliente de WhatsApp: {}", ex.getMessage(), ex);
			return ASSISTANT_UNAVAILABLE_REPLY;
		}
	}

	private Lock lockFor(String conversationKey) {
		return conversationLocks[Math.floorMod(conversationKey.hashCode(), LOCK_STRIPES)];
	}
}
