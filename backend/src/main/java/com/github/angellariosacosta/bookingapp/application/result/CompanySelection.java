package com.github.angellariosacosta.bookingapp.application.result;

/** Negocio elegido en una conversación; {@code ownerUserId} es el tenant bajo el que se agenda. */
public record CompanySelection(Long ownerUserId, String companyName) {
}
