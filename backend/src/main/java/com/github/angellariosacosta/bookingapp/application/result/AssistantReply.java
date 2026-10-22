package com.github.angellariosacosta.bookingapp.application.result;

/** Respuesta del asistente con los tokens que el proveedor cobró por generarla (suma de todas sus llamadas internas). */
public record AssistantReply(String text, long tokensUsed) {
}
