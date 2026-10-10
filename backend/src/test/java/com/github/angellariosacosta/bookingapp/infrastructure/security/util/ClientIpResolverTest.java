package com.github.angellariosacosta.bookingapp.infrastructure.security.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.angellariosacosta.bookingapp.infrastructure.config.ClientIpProperties;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

	private static final String SOCKET_IP = "10.0.0.5";
	private static final String FORWARDED_FOR = "X-Forwarded-For";

	private ClientIpResolver resolverWithHops(int hops) {
		ClientIpProperties properties = new ClientIpProperties();
		properties.setTrustedProxyHops(hops);
		return new ClientIpResolver(properties);
	}

	private MockHttpServletRequest request(String forwardedFor) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(SOCKET_IP);
		if (forwardedFor != null) {
			request.addHeader(FORWARDED_FOR, forwardedFor);
		}
		return request;
	}

	@Test
	void zeroHopsIgnoresHeaderAndUsesSocketAddress() {
		assertEquals(SOCKET_IP, resolverWithHops(0).resolve(request("1.2.3.4")));
	}

	@Test
	void oneHopTakesEntryBeforeTheTrustedProxy() {
		assertEquals("203.0.113.9", resolverWithHops(1).resolve(request("203.0.113.9, 130.176.0.1")));
	}

	@Test
	void spoofedLeadingEntriesAreIgnored() {
		assertEquals("203.0.113.9", resolverWithHops(1).resolve(request("6.6.6.6, 203.0.113.9, 130.176.0.1")));
	}

	@Test
	void fallsBackToSocketAddressWhenHeaderIsMissingOrTooShort() {
		assertEquals(SOCKET_IP, resolverWithHops(1).resolve(request(null)));
		assertEquals(SOCKET_IP, resolverWithHops(1).resolve(request("130.176.0.1")));
	}
}
