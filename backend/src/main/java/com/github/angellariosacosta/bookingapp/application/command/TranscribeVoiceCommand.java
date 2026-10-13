package com.github.angellariosacosta.bookingapp.application.command;

/** Audio grabado por el usuario tal como llega del cliente; el caso de uso lo valida. */
public record TranscribeVoiceCommand(Long userId, byte[] audio, String contentType) {
}
