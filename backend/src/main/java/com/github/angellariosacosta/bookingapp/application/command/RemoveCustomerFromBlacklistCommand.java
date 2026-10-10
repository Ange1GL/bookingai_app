package com.github.angellariosacosta.bookingapp.application.command;

public record RemoveCustomerFromBlacklistCommand(Long customerId, Long userId) {
}
