package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.ai;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.HandleCustomerMessageCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerAssistantPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerConversationPort;
import com.github.angellariosacosta.bookingapp.application.result.CompanySelection;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.ai.tool.CustomerBookingTools;

@Component
public class SpringAiCustomerAssistantAdapter implements CustomerAssistantPort {

	private static final String MEMORY_PREFIX = "wa:";
	private static final String EMPTY_REPLY_FALLBACK = "No te entendí bien, ¿puedes repetirlo?";

	private static final String SYSTEM_PROMPT = """
			Eres el asistente de citas de la app Aglacitas por WhatsApp. Hablas con clientes finales de negocios
			(barberías y similares) que quieren agendar una cita. Responde de forma breve, amable y en español.

			Flujo:
			1. Si el cliente saluda y aún no hay negocio elegido, responde: "Bienvenido a la app de Aglacitas, por favor
			   ingresa el nombre del negocio donde quieres agendar tu cita".
			2. El cliente puede dar el nombre del negocio o el nombre del dueño. Si da el del negocio, busca con
			   findCompanyByName; si da el de una persona, busca con findCompanyByOwnerName; si no queda claro, prueba
			   con ambas. La búsqueda no distingue mayúsculas y acepta coincidencias parciales.
			   - Sin resultados: dile que ese negocio o dueño no está registrado en la plataforma y pídele que revise
			     el nombre o pruebe con el otro dato.
			   - Varios resultados (incluso varios dueños con el mismo nombre): muéstrale la lista con el nombre del
			     negocio y el del dueño de cada uno y pídele que elija.
			   - Un solo resultado: confírmalo con el cliente antes de continuar.
			   Cuando confirme cuál es, usa selectCompany con su businessId.
			3. Con el negocio elegido, llama a getMyProfile: si known es false, pídele su nombre completo.
			4. Muestra los servicios con listServices (nombre y precio) y pregunta cuál quiere, además de día y hora.
			   Si usa fechas relativas (hoy, mañana, el viernes), llama primero a getCurrentDateTime.
			5. Confirma servicio, día y hora y entonces agenda con bookMyAppointment. No registres citas en el pasado.
			   Si el horario está ocupado, sugiere el siguiente más cercano.

			Reglas: solo puedes agendar citas; no puedes ver, mover ni cancelar citas, ni dar datos de otros clientes.
			Si la herramienta indica que no puede agendar con ese negocio, avísale sin dar detalles y sugiere que
			contacte directamente al negocio. Nunca inventes negocios, servicios, precios ni horarios.
			Si piden algo fuera de agendar una cita, explica amablemente que solo puedes ayudar a agendar.
			""";

	private final ChatClient chatClient;
	private final CustomerConversationPort conversations;

	public SpringAiCustomerAssistantAdapter(@Qualifier("customerChatClient") ChatClient chatClient,
			CustomerConversationPort conversations) {
		this.chatClient = chatClient;
		this.conversations = conversations;
	}

	@Override
	public String reply(HandleCustomerMessageCommand message) {
		String conversationKey = message.conversationKey();
		String reply = chatClient.prompt()
				.system(systemPrompt(conversationKey))
				.user(message.text())
				.advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, MEMORY_PREFIX + conversationKey))
				.toolContext(Map.of(
						CustomerBookingTools.CONVERSATION_KEY, conversationKey,
						CustomerBookingTools.CUSTOMER_PHONE, message.from()))
				.call()
				.content();
		return reply == null || reply.isBlank() ? EMPTY_REPLY_FALLBACK : reply;
	}

	// El estado vive en el servidor: se le recuerda al modelo en cada turno qué negocio hay elegido (si hay).
	private String systemPrompt(String conversationKey) {
		String state = conversations.findSelection(conversationKey)
				.map(this::selectedState)
				.orElse("Estado actual: aún no hay negocio elegido.");
		return SYSTEM_PROMPT + "\n" + state;
	}

	private String selectedState(CompanySelection selection) {
		return "Estado actual: el negocio elegido es \"%s\" (businessId %d); no vuelvas a pedirlo salvo que el cliente quiera cambiarlo."
				.formatted(selection.companyName(), selection.ownerUserId());
	}
}
