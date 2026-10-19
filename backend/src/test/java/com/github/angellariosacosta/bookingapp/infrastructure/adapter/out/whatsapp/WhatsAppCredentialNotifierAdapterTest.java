package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.whatsapp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.github.angellariosacosta.bookingapp.infrastructure.config.WhatsAppCloudProperties;

class WhatsAppCredentialNotifierAdapterTest {

	private static final String BASE_URL = "https://graph.test";
	private static final String TOKEN = "test-token";
	private static final String SENDER_ID = "2002";
	private static final String ENDPOINT = BASE_URL + "/v26.0/2002/messages";
	private static final String PHONE = "5215512345678";

	private MockRestServiceServer server;
	private WhatsAppCloudProperties properties;
	private WhatsAppCredentialNotifierAdapter adapter;

	@BeforeEach
	void setUp() {
		properties = new WhatsAppCloudProperties();
		properties.setSystemUserToken(TOKEN);
		properties.setCredentialsPhoneNumberId(SENDER_ID);
		properties.setCredentialsTemplateName("user_credentials");
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new WhatsAppCredentialNotifierAdapter(builder.build(), properties);
	}

	@Test
	void sendsTemplateWithUsernameAndPasswordAsBodyParameters() {
		server.expect(requestTo(ENDPOINT))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer " + TOKEN))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.messaging_product").value("whatsapp"))
				.andExpect(jsonPath("$.to").value(PHONE))
				.andExpect(jsonPath("$.type").value("template"))
				.andExpect(jsonPath("$.template.name").value("user_credentials"))
				.andExpect(jsonPath("$.template.language.code").value("es"))
				.andExpect(jsonPath("$.template.components[0].type").value("body"))
				.andExpect(jsonPath("$.template.components[0].parameters[0].text").value("angel"))
				.andExpect(jsonPath("$.template.components[0].parameters[1].text").value("S3cret!pass"))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		assertTrue(adapter.sendTemporaryPassword(PHONE, "angel", "S3cret!pass"));

		server.verify();
	}

	@Test
	void returnsFalseWhenMetaRejects() {
		server.expect(requestTo(ENDPOINT))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\":{\"code\":132000}}")
						.contentType(MediaType.APPLICATION_JSON));

		assertFalse(adapter.sendTemporaryPassword(PHONE, "angel", "S3cret!pass"));

		server.verify();
	}

	@Test
	void returnsFalseOnServerError() {
		server.expect(requestTo(ENDPOINT)).andRespond(withServerError());

		assertFalse(adapter.sendTemporaryPassword(PHONE, "angel", "S3cret!pass"));
	}

	@Test
	void doesNotCallMetaWhenSenderIdIsNotNumeric() {
		properties.setCredentialsPhoneNumberId("../evil");

		assertFalse(adapter.sendTemporaryPassword(PHONE, "angel", "S3cret!pass"));

		server.verify();
	}

	@Test
	void senderIsEnabledOnlyWhenTokenSenderAndTemplateAreConfigured() {
		assertTrue(properties.isCredentialsSenderEnabled());

		properties.setCredentialsTemplateName(" ");
		assertFalse(properties.isCredentialsSenderEnabled());

		properties.setCredentialsTemplateName("user_credentials");
		properties.setCredentialsPhoneNumberId("");
		assertFalse(properties.isCredentialsSenderEnabled());

		properties.setCredentialsPhoneNumberId(SENDER_ID);
		properties.setSystemUserToken("");
		assertFalse(properties.isCredentialsSenderEnabled());
	}
}
