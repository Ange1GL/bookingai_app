package com.github.angellariosacosta.bookingapp.application.command;

public record CreateCustomerCommand(
		String name,
		String phone
) {
}
