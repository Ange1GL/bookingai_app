package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.application.result.PushDeliveryStatus;
import com.github.angellariosacosta.bookingapp.application.result.PushMessage;
import com.github.angellariosacosta.bookingapp.domain.model.PushSubscription;

public interface WebPushSenderPort {

	/** Nunca lanza: un fallo de un dispositivo no debe frenar los demás. */
	PushDeliveryStatus send(PushSubscription subscription, PushMessage message);
}
