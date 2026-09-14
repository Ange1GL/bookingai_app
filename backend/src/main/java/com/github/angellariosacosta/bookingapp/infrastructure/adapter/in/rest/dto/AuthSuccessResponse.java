package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.util.List;

public record AuthSuccessResponse(Long userId, String email, List<String> roles) {}
