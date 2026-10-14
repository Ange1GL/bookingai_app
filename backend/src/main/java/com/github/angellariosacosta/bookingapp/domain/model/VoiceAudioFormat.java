package com.github.angellariosacosta.bookingapp.domain.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Formatos de audio que graba {@code MediaRecorder} en los navegadores soportados y que acepta el
 * proveedor de transcripción. Además del MIME declarado por el cliente se comprueba la firma
 * (magic bytes) del archivo, para no enviar al proveedor algo que no es audio.
 */
public enum VoiceAudioFormat {

	// Chrome, Edge, Firefox, Android.
	WEBM("audio/webm", "webm", 0, new byte[] {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3}),
	// Firefox (algunas versiones).
	OGG("audio/ogg", "ogg", 0, "OggS".getBytes(StandardCharsets.US_ASCII)),
	// Safari (macOS / iOS): contenedor MP4 con la caja "ftyp" a partir del byte 4.
	MP4("audio/mp4", "mp4", 4, "ftyp".getBytes(StandardCharsets.US_ASCII));

	private static final char MIME_PARAMETERS_SEPARATOR = ';';

	private final String mimeType;
	private final String extension;
	private final int signatureOffset;
	private final byte[] signature;

	VoiceAudioFormat(String mimeType, String extension, int signatureOffset, byte[] signature) {
		this.mimeType = mimeType;
		this.extension = extension;
		this.signatureOffset = signatureOffset;
		this.signature = signature;
	}

	/** Resuelve el formato a partir del Content-Type, ignorando parámetros como {@code ;codecs=opus}. */
	public static Optional<VoiceAudioFormat> fromContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return Optional.empty();
		}
		String baseType = stripParameters(contentType).strip().toLowerCase(Locale.ROOT);
		return Arrays.stream(values()).filter(format -> format.mimeType.equals(baseType)).findFirst();
	}

	public boolean matchesSignature(byte[] audio) {
		if (audio == null || audio.length < signatureOffset + signature.length) {
			return false;
		}
		byte[] header = Arrays.copyOfRange(audio, signatureOffset, signatureOffset + signature.length);
		return Arrays.equals(header, signature);
	}

	public String extension() {
		return extension;
	}

	private static String stripParameters(String contentType) {
		int separator = contentType.indexOf(MIME_PARAMETERS_SEPARATOR);
		return separator < 0 ? contentType : contentType.substring(0, separator);
	}
}
