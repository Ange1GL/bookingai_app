package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand;
import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand.IncomingMessage;
import com.github.angellariosacosta.bookingapp.application.port.in.ReceiveWhatsAppEventUseCase;
import com.github.angellariosacosta.bookingapp.infrastructure.config.WhatsAppProperties;
import com.github.angellariosacosta.bookingapp.infrastructure.security.util.WebhookSignatureVerifier;

import tools.jackson.databind.json.JsonMapper;

class WhatsAppWebhookControllerTest {

	private static final String PATH = WhatsAppWebhookController.WEBHOOK_PATH;
	private static final String VERIFY_TOKEN = "verify-token";
	private static final String APP_SECRET = "app-secret";
	private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

	private static final String MESSAGE_PAYLOAD = """
			{"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"messages","value":{
			"metadata":{"phone_number_id":"123456","display_phone_number":"5215500000000"},
			"contacts":[{"wa_id":"5215511111111","profile":{"name":"Cliente"}}],
			"messages":[{"from":"5215511111111","id":"wamid.XXX","timestamp":"1700000000","type":"text","text":{"body":"Hola"}}]}}]}]}
			""";
	private static final String STATUS_PAYLOAD = """
			{"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"messages","value":{
			"metadata":{"phone_number_id":"123456"},"statuses":[{"id":"wamid.XXX","status":"delivered"}]}}]}]}
			""";

	private ReceiveWhatsAppEventUseCase useCase;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		WhatsAppProperties properties = new WhatsAppProperties();
		properties.setVerifyToken(VERIFY_TOKEN);
		properties.setAppSecret(APP_SECRET);
		useCase = mock(ReceiveWhatsAppEventUseCase.class);
		WhatsAppWebhookController controller = new WhatsAppWebhookController(
				properties, new WebhookSignatureVerifier(properties), useCase, JsonMapper.builder().build());
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void verifyReturnsChallengeAsPlainText() throws Exception {
		mockMvc.perform(get(PATH)
						.param("hub.mode", "subscribe")
						.param("hub.verify_token", VERIFY_TOKEN)
						.param("hub.challenge", "1158201444"))
				.andExpect(status().isOk())
				.andExpect(content().string("1158201444"));
	}

	@Test
	void verifyRejectsWrongToken() throws Exception {
		mockMvc.perform(get(PATH)
						.param("hub.mode", "subscribe")
						.param("hub.verify_token", "otro")
						.param("hub.challenge", "123"))
				.andExpect(status().isForbidden());
	}

	@Test
	void verifyRejectsWrongModeAndMissingParams() throws Exception {
		mockMvc.perform(get(PATH)
						.param("hub.mode", "unsubscribe")
						.param("hub.verify_token", VERIFY_TOKEN)
						.param("hub.challenge", "123"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(PATH)).andExpect(status().isForbidden());
	}

	@Test
	void receiveRejectsMissingOrInvalidSignature() throws Exception {
		mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(MESSAGE_PAYLOAD))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(MESSAGE_PAYLOAD)
						.header(SIGNATURE_HEADER, "sha256=deadbeef"))
				.andExpect(status().isUnauthorized());
		verify(useCase, never()).receive(any());
	}

	@Test
	void receiveDispatchesMessagesWhenSignatureIsValid() throws Exception {
		mockMvc.perform(signedPost(MESSAGE_PAYLOAD)).andExpect(status().isOk());

		IncomingMessage message = capturedCommand().messages().getFirst();
		assertEquals("123456", message.phoneNumberId());
		assertEquals("5215511111111", message.from());
		assertEquals("wamid.XXX", message.messageId());
		assertEquals("Hola", message.text());
	}

	@Test
	void receiveStatusEventsProducesNoMessages() throws Exception {
		mockMvc.perform(signedPost(STATUS_PAYLOAD)).andExpect(status().isOk());

		assertTrue(capturedCommand().messages().isEmpty());
	}

	@Test
	void receiveAnswersOkWhenSignedPayloadIsMalformed() throws Exception {
		mockMvc.perform(signedPost("{no es json")).andExpect(status().isOk());

		verify(useCase, never()).receive(any());
	}

	private ReceiveWhatsAppEventCommand capturedCommand() {
		ArgumentCaptor<ReceiveWhatsAppEventCommand> captor = ArgumentCaptor.forClass(ReceiveWhatsAppEventCommand.class);
		verify(useCase).receive(captor.capture());
		return captor.getValue();
	}

	private static MockHttpServletRequestBuilder signedPost(String body) throws Exception {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		return post(PATH).contentType(MediaType.APPLICATION_JSON).content(bytes)
				.header(SIGNATURE_HEADER, signature(bytes));
	}

	private static String signature(byte[] body) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
	}
}
