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

class WhatsAppOneTimeCodeNotifierAdapterTest {

	private static final String BASE_URL = "https://graph.test";
	private static final String TOKEN = "test-token";
	private static final String SENDER_ID = "2002";
	private static final String ENDPOINT = BASE_URL + "/v26.0/2002/messages";
	private static final String PHONE = "5215512345678";

	private MockRestServiceServer server;
	private WhatsAppCloudProperties properties;
	private WhatsAppOneTimeCodeNotifierAdapter adapter;

	@BeforeEach
	void setUp() {
		properties = new WhatsAppCloudProperties();
		properties.setSystemUserToken(TOKEN);
		properties.setOtpPhoneNumberId(SENDER_ID);
		properties.setOtpTemplateName("codigo_verificacion");
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new WhatsAppOneTimeCodeNotifierAdapter(builder.build(), properties);
	}

	@Test
	void sendsAnAuthenticationTemplateWithTheCodeInBodyAndCopyButton() {
		server.expect(requestTo(ENDPOINT))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer " + TOKEN))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.messaging_product").value("whatsapp"))
				.andExpect(jsonPath("$.to").value(PHONE))
				.andExpect(jsonPath("$.type").value("template"))
				.andExpect(jsonPath("$.template.name").value("codigo_verificacion"))
				.andExpect(jsonPath("$.template.language.code").value("es"))
				.andExpect(jsonPath("$.template.components[0].type").value("body"))
				.andExpect(jsonPath("$.template.components[0].parameters[0].text").value("483920"))
				.andExpect(jsonPath("$.template.components[0].sub_type").doesNotExist())
				.andExpect(jsonPath("$.template.components[0].index").doesNotExist())
				.andExpect(jsonPath("$.template.components[1].type").value("button"))
				.andExpect(jsonPath("$.template.components[1].sub_type").value("url"))
				.andExpect(jsonPath("$.template.components[1].index").value("0"))
				.andExpect(jsonPath("$.template.components[1].parameters[0].text").value("483920"))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		assertTrue(adapter.sendCode(PHONE, "483920"));

		server.verify();
	}

	@Test
	void returnsFalseWhenMetaRejects() {
		server.expect(requestTo(ENDPOINT))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\":{\"code\":132000}}")
						.contentType(MediaType.APPLICATION_JSON));

		assertFalse(adapter.sendCode(PHONE, "483920"));

		server.verify();
	}

	@Test
	void returnsFalseOnServerError() {
		server.expect(requestTo(ENDPOINT)).andRespond(withServerError());

		assertFalse(adapter.sendCode(PHONE, "483920"));
	}

	@Test
	void doesNotCallMetaWhenSenderIdIsNotNumeric() {
		properties.setOtpPhoneNumberId("../evil");

		assertFalse(adapter.sendCode(PHONE, "483920"));

		server.verify();
	}

	@Test
	void senderIsEnabledOnlyWhenTokenSenderAndTemplateAreConfigured() {
		assertTrue(properties.isOtpSenderEnabled());

		properties.setOtpTemplateName(" ");
		assertFalse(properties.isOtpSenderEnabled());

		properties.setOtpTemplateName("codigo_verificacion");
		properties.setOtpPhoneNumberId("");
		assertFalse(properties.isOtpSenderEnabled());

		properties.setOtpPhoneNumberId(SENDER_ID);
		properties.setSystemUserToken("");
		assertFalse(properties.isOtpSenderEnabled());
	}
}
