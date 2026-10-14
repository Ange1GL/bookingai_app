package com.github.angellariosacosta.bookingapp.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class VoiceAudioFormatTest {

	private static final byte[] WEBM_HEADER = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0x01, 0x02};
	private static final byte[] OGG_HEADER = "OggS\0\2".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] MP4_HEADER = "\0\0\0\u0018ftypmp42".getBytes(StandardCharsets.US_ASCII);

	@DisplayName("Should resolve the format ignoring codec parameters and case")
	@Test
	void shouldResolveFormatIgnoringParameters() {
		assertEquals(Optional.of(VoiceAudioFormat.WEBM), VoiceAudioFormat.fromContentType("audio/webm;codecs=opus"));
		assertEquals(Optional.of(VoiceAudioFormat.OGG), VoiceAudioFormat.fromContentType("Audio/OGG; codecs=opus"));
		assertEquals(Optional.of(VoiceAudioFormat.MP4), VoiceAudioFormat.fromContentType("audio/mp4"));
	}

	@DisplayName("Should not resolve unsupported or missing content types")
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"audio/wav", "video/webm", "application/octet-stream"})
	void shouldNotResolveUnsupportedContentTypes(String contentType) {
		assertTrue(VoiceAudioFormat.fromContentType(contentType).isEmpty());
	}

	@DisplayName("Should accept audio whose bytes match the format signature")
	@Test
	void shouldMatchSignature() {
		assertTrue(VoiceAudioFormat.WEBM.matchesSignature(WEBM_HEADER));
		assertTrue(VoiceAudioFormat.OGG.matchesSignature(OGG_HEADER));
		assertTrue(VoiceAudioFormat.MP4.matchesSignature(MP4_HEADER));
	}

	@DisplayName("Should reject audio whose bytes do not match the declared format")
	@Test
	void shouldRejectMismatchedSignature() {
		assertFalse(VoiceAudioFormat.WEBM.matchesSignature(OGG_HEADER));
		assertFalse(VoiceAudioFormat.MP4.matchesSignature(WEBM_HEADER));
		assertFalse(VoiceAudioFormat.OGG.matchesSignature(new byte[] {'O', 'g'}));
		assertFalse(VoiceAudioFormat.OGG.matchesSignature(null));
	}
}
