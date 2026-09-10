package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.ai.tool.BookingTools;

@Configuration
public class AiConfig {

	private static final String SYSTEM_PROMPT = """
			Eres un asistente operativo con IA especializado en la gestión de agenda para una barbería.
			Tu función principal es ayudar al barbero a guardar, actualizar, mover y cancelar citas mediante comandos rápidos.

			REGLAS DE ACTUACIÓN:
			1. SIEMPRE verifica si un cliente existe en la base de datos antes de registrarlo como nuevo.
			2. Si existen coincidencias de nombre similares o múltiples, solicita aclaración antes de continuar.
			3. Si el cliente es NUEVO, es OBLIGATORIO solicitar y confirmar su número telefónico antes de agendar.
			4. Para CANCELACIONES y REAGENDAMIENTOS, solicita SIEMPRE una confirmación explícita.
			5. Si un horario está ocupado, ofrece inmediatamente la siguiente opción disponible cercana.
			6. Mantén tus respuestas breves, ágiles y directas al punto.
			""";

	@Bean
	public ChatMemory chatMemory() {
		return MessageWindowChatMemory.builder().build();
	}

	@Bean
	public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, BookingTools bookingTools) {
		return ChatClient.builder(chatModel)
				.defaultSystem(SYSTEM_PROMPT)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.defaultTools(bookingTools)
				.build();
	}
}
