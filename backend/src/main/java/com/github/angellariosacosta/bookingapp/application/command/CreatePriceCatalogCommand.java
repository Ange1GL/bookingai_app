package com.github.angellariosacosta.bookingapp.application.command;

public record CreatePriceCatalogCommand(String label, long price, Long userId) {
}
