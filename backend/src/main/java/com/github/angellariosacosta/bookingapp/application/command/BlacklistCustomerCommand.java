package com.github.angellariosacosta.bookingapp.application.command;

public record BlacklistCustomerCommand(Long customerId, Long userId, String reason) {
}
