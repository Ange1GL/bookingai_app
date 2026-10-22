package com.github.angellariosacosta.bookingapp.application.result;

public enum PushDeliveryStatus {
	SENT,
	/** El servicio push respondió 404/410: el usuario revocó el permiso o la suscripción caducó. */
	GONE,
	FAILED
}
