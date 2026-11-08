package com.github.angellariosacosta.bookingapp.application.port.in;

import java.util.List;

import com.github.angellariosacosta.bookingapp.application.result.UserSessionResult;

public interface ListUserSessionsUseCase {

	/** Sesiones activas del usuario, la más reciente primero. */
	List<UserSessionResult> listActive(Long userId);
}
