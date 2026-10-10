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
			Si la búsqueda muestra que el cliente está en la lista negra (blacklisted = true), NO agendes. Avisa al usuario
			que ese cliente está bloqueado y pregúntale si quiere quitarlo de la lista negra o mantenerlo bloqueado.
			Si responde que lo quite, usa la herramienta para quitarlo y continúa con la cita; si lo mantiene, no agendes.
			Para agregar un cliente a la lista negra, identifícalo primero buscándolo por nombre (si hay varias coincidencias,
			pide aclarar), confirma con el usuario avisando que se cancelarán sus citas futuras, y pregunta si desea indicar
			un motivo (opcional, máximo 250 caracteres; no inventes uno).
			Si al agendar la herramienta responde que el cliente está en la lista negra, no reintentes: aplica el mismo aviso.
			Antes de agendar una cita, llama a la herramienta del catálogo de precios, muestra los servicios disponibles
			(nombre y precio) y pregunta cuál se aplicará. Pide también nombre completo y teléfono si el cliente es nuevo.
			No agendes sin que el usuario haya elegido un servicio del catálogo.
			Antes de eliminar o mover una cita, confirma la acción con el usuario.
			Si el horario solicitado no está disponible, sugiere la siguiente opción más cercana.
			Si el usuario usa fechas o horas relativas (hoy, mañana, el viernes), llama primero a la herramienta de fecha y hora actual.
			No registres citas en el pasado.
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
