package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.util.List;

import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand;
import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand.IncomingMessage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Subconjunto del payload del webhook de WhatsApp Cloud API que usamos; los campos desconocidos se ignoran. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhookPayload(List<Entry> entry) {

	public record Entry(List<Change> changes) {}

	public record Change(String field, Value value) {}

	public record Value(Metadata metadata, List<Message> messages) {}

	public record Metadata(@JsonProperty("phone_number_id") String phoneNumberId) {}

	public record Message(String from, String id, String type, Text text) {}

	public record Text(String body) {}

	public ReceiveWhatsAppEventCommand toCommand() {
		List<IncomingMessage> messages = nullSafe(entry).stream()
				.flatMap(e -> nullSafe(e.changes()).stream())
				.filter(change -> change.value() != null)
				.flatMap(change -> nullSafe(change.value().messages()).stream()
						.map(m -> new IncomingMessage(
								phoneNumberId(change.value()),
								m.from(),
								m.id(),
								m.type(),
								m.text() == null ? null : m.text().body())))
				.toList();
		return new ReceiveWhatsAppEventCommand(messages);
	}

	private static String phoneNumberId(Value value) {
		return value.metadata() == null ? null : value.metadata().phoneNumberId();
	}

	private static <T> List<T> nullSafe(List<T> list) {
		return list == null ? List.of() : list;
	}
}
