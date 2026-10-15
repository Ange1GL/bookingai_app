package com.github.angellariosacosta.bookingapp.domain.shared;

/** Enmascara teléfonos para logs: nunca se escribe el número completo (dato personal). */
public final class PhoneMask {

	private static final int VISIBLE_PHONE_DIGITS = 4;
	private static final String PHONE_MASK = "***";

	private PhoneMask() {
	}

	public static String mask(String phone) {
		if (phone == null || phone.length() <= VISIBLE_PHONE_DIGITS) {
			return PHONE_MASK;
		}
		return PHONE_MASK + phone.substring(phone.length() - VISIBLE_PHONE_DIGITS);
	}
}
