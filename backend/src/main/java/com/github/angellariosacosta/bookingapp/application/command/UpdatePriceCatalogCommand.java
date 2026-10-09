package com.github.angellariosacosta.bookingapp.application.command;

public record UpdatePriceCatalogCommand(Integer id, String label, long price, Long userId) {
}
