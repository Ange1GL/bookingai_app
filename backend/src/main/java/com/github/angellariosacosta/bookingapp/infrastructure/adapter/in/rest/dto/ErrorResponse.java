package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

public record ErrorResponse(String timestamp, int status, String error, String message) {}
