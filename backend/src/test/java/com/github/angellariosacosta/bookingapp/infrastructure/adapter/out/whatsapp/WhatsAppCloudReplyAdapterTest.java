package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.github.angellariosacosta.bookingapp.infrastructure.config.WhatsAppCloudProperties;

class WhatsAppCloudReplyAdapterTest {

	private static final String BASE_URL = "https://graph.test";
	private static final String TOKEN = "test-token";
	private static final String PHONE_NUMBER_ID = "1001";
	private static final String TO = "5215512345678";
	private static final String ENDPOINT = BASE_URL + "/v26.0/1001/messages";

	private MockRestServiceServer server;
	private WhatsAppCloudReplyAdapter adapter;

	@BeforeEach
	void setUp() {
		WhatsAppCloudProperties properties = new WhatsAppCloudProperties();
		properties.setSystemUserToken(TOKEN);
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new WhatsAppCloudReplyAdapter(builder.build(), properties);
	}

	@DisplayName("Should post a text message with the bearer token to the phone number endpoint")
	@Test
	void shouldPostTextMessage() {
		server.expect(requestTo(ENDPOINT))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer " + TOKEN))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.messaging_product").value("whatsapp"))
				.andExpect(jsonPath("$.to").value(TO))
				.andExpect(jsonPath("$.type").value("text"))
				.andExpect(jsonPath("$.text.body").value("Hola"))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		adapter.send(PHONE_NUMBER_ID, TO, "Hola");

		server.verify();
	}

	@DisplayName("Should truncate texts longer than the WhatsApp limit")
	@Test
	void shouldTruncateLongText() {
		String longText = "a".repeat(WhatsAppCloudReplyAdapter.MAX_TEXT_LENGTH + 50);
		server.expect(requestTo(ENDPOINT))
				.andExpect(jsonPath("$.text.body").value("a".repeat(WhatsAppCloudReplyAdapter.MAX_TEXT_LENGTH)))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		adapter.send(PHONE_NUMBER_ID, TO, longText);

		server.verify();
	}

	@DisplayName("Should not throw when Meta rejects the message")
	@Test
	void shouldNotThrowWhenMetaRejects() {
		server.expect(requestTo(ENDPOINT))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\":{\"code\":131047}}")
						.contentType(MediaType.APPLICATION_JSON));

		assertDoesNotThrow(() -> adapter.send(PHONE_NUMBER_ID, TO, "Hola"));

		server.verify();
	}

	@DisplayName("Should not throw when Meta fails with a server error")
	@Test
	void shouldNotThrowOnServerError() {
		server.expect(ExpectedCount.once(), requestTo(ENDPOINT)).andRespond(withServerError());

		assertDoesNotThrow(() -> adapter.send(PHONE_NUMBER_ID, TO, "Hola"));

		server.verify();
	}

	@DisplayName("Should not call Meta when the phone number id is not numeric")
	@Test
	void shouldRejectInvalidPhoneNumberId() {
		adapter.send("../evil", TO, "Hola");

		server.verify();
	}
}
