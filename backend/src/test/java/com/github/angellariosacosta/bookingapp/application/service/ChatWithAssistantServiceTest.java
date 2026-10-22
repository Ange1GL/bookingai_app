package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.command.ChatWithAssistantCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.AiUsageRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.AssistantChatPort;
import com.github.angellariosacosta.bookingapp.application.result.AssistantReply;
import com.github.angellariosacosta.bookingapp.application.result.ChatWithAssistantResult;
import com.github.angellariosacosta.bookingapp.domain.exception.AiQuotaExceededException;
import com.github.angellariosacosta.bookingapp.domain.exception.AssistantUnavailableException;

@ExtendWith(MockitoExtension.class)
class ChatWithAssistantServiceTest {

	private static final Long USER_ID = 7L;
	private static final String SESSION_ID = "session-1";
	private static final String MESSAGE = "Agenda a Juan mañana a las cinco";
	private static final long USER_LIMIT = 1000;
	private static final long GLOBAL_LIMIT = 5000;
	private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Mexico_City");
	// 2026-10-14T03:00Z todavía es 13 de octubre en Ciudad de México (UTC-6).
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T03:00:00Z"), BUSINESS_ZONE);
	private static final LocalDate BUSINESS_TODAY = LocalDate.of(2026, 10, 13);
	private static final ChatWithAssistantCommand COMMAND = new ChatWithAssistantCommand(USER_ID, SESSION_ID, MESSAGE);

	@Mock
	private AssistantChatPort assistant;

	@Mock
	private AiUsageRepositoryPort usage;

	private ChatWithAssistantService service;

	@BeforeEach
	void setUp() {
		AiCreditsTracker tracker = new AiCreditsTracker(usage, new AiCreditsPolicy(USER_LIMIT, GLOBAL_LIMIT), CLOCK);
		service = new ChatWithAssistantService(assistant, tracker);
	}

	@Test
	@DisplayName("cobra los tokens reales y devuelve el saldo actualizado")
	void chargesRealTokensAndReturnsCredits() {
		when(usage.usedByUser(USER_ID, BUSINESS_TODAY)).thenReturn(100L, 350L);
		when(usage.usedGlobally(BUSINESS_TODAY)).thenReturn(100L);
		when(assistant.chat(USER_ID, SESSION_ID, MESSAGE)).thenReturn(new AssistantReply("Listo", 250));

		ChatWithAssistantResult result = service.chat(COMMAND);

		verify(usage).addUsage(USER_ID, BUSINESS_TODAY, 250);
		assertEquals("Listo", result.reply());
		assertEquals(650, result.credits().remaining());
		assertEquals(35, result.credits().percentUsed());
		assertEquals(OffsetDateTime.parse("2026-10-14T00:00:00-06:00"), result.credits().resetsAt());
	}

	@Test
	@DisplayName("sin saldo del usuario no llama al proveedor ni cobra")
	void exhaustedUserDoesNotCallProvider() {
		when(usage.usedByUser(USER_ID, BUSINESS_TODAY)).thenReturn(USER_LIMIT);

		assertThrows(AiQuotaExceededException.class, () -> service.chat(COMMAND));

		verifyNoInteractions(assistant);
		verify(usage, never()).addUsage(anyLong(), any(), anyLong());
	}

	@Test
	@DisplayName("con el tope global agotado no llama al proveedor aunque el usuario tenga saldo")
	void exhaustedGlobalLimitDoesNotCallProvider() {
		when(usage.usedByUser(USER_ID, BUSINESS_TODAY)).thenReturn(0L);
		when(usage.usedGlobally(BUSINESS_TODAY)).thenReturn(GLOBAL_LIMIT);

		AiQuotaExceededException ex = assertThrows(AiQuotaExceededException.class, () -> service.chat(COMMAND));

		assertEquals(AiCreditsTracker.GLOBAL_LIMIT_MESSAGE, ex.getMessage());
		verifyNoInteractions(assistant);
	}

	@Test
	@DisplayName("si el proveedor falla no se cobra nada")
	void providerFailureIsNotCharged() {
		when(usage.usedByUser(USER_ID, BUSINESS_TODAY)).thenReturn(0L);
		when(usage.usedGlobally(BUSINESS_TODAY)).thenReturn(0L);
		when(assistant.chat(USER_ID, SESSION_ID, MESSAGE)).thenThrow(new AssistantUnavailableException(new RuntimeException("boom")));

		assertThrows(AssistantUnavailableException.class, () -> service.chat(COMMAND));

		verify(usage, never()).addUsage(anyLong(), any(), anyLong());
	}

	@Test
	@DisplayName("el último mensaje puede pasarse del límite y el saldo queda en cero, no negativo")
	void overshootLeavesZeroRemaining() {
		when(usage.usedByUser(USER_ID, BUSINESS_TODAY)).thenReturn(900L, 1200L);
		when(usage.usedGlobally(BUSINESS_TODAY)).thenReturn(0L);
		when(assistant.chat(USER_ID, SESSION_ID, MESSAGE)).thenReturn(new AssistantReply("Listo", 300));

		ChatWithAssistantResult result = service.chat(COMMAND);

		assertEquals(0, result.credits().remaining());
		assertEquals(100, result.credits().percentUsed());
	}
}
