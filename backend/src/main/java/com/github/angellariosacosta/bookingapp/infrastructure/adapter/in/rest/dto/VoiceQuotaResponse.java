package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.time.OffsetDateTime;

public record VoiceQuotaResponse(int dailyLimit, int used, int remaining, int percentUsed, boolean monthlyExhausted,
                                 OffsetDateTime resetsAt) {}
