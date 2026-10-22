package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.result.VoiceQuota;

public interface GetVoiceQuotaUseCase {

	VoiceQuota getQuota(Long userId);
}
