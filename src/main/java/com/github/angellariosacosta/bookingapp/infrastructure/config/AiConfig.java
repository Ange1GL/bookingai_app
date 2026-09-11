package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.ai.tool.BookingTools;

@Configuration
public class AiConfig {

	private static final String SYSTEM_PROMPT = """
			Eres un asistente de agenda para una barbería. Ayudas a registrar, mover y eliminar citas de forma rápida.

			Antes de registrar a un cliente, verifica si ya existe en la base de datos buscando por nombre.
			Si hay varias coincidencias similares, pide al usuario que aclare cuál es el cliente correcto.
			Cuando el cliente no existe, solicita su número de teléfono para crearlo antes de agendar la cita.
			Antes de eliminar o mover una cita, confirma la acción con el usuario.
			Si el horario solicitado no está disponible, sugiere la siguiente opción más cercana.
			Responde de forma breve y directa.
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

	@Bean
	@Qualifier("rawChatClient")
	public ChatClient rawChatClient(ChatModel chatModel) {
		return ChatClient.builder(chatModel).build();
	}
}
