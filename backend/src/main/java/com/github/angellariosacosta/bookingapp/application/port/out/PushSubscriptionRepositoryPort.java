package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.List;

import com.github.angellariosacosta.bookingapp.domain.model.PushSubscription;

public interface PushSubscriptionRepositoryPort {

	/** Guarda o actualiza por {@code endpoint} (único): reinstalar o renovar la suscripción no duplica el dispositivo. */
	void upsert(PushSubscription subscription);

	List<PushSubscription> findByUserId(Long userId);

	void deleteByUserIdAndEndpoint(Long userId, String endpoint);

	void deleteByEndpoint(String endpoint);
}
