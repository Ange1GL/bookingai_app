package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.OffsetDateTime;

public record CreditsResponse(long dailyLimit, long used, long remaining, int percentUsed, OffsetDateTime resetsAt) {}
