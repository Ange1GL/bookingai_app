package com.github.angellariosacosta.bookingapp.domain.shared;

import java.util.regex.Pattern;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;

/** Teléfonos en formato internacional solo con dígitos (E.164 sin el "+"), como los usa WhatsApp. */
public final class PhoneNumbers {

	public static final int MIN_DIGITS = 8;
	public static final int MAX_DIGITS = 15;
	private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-().]");
	private static final Pattern DIGITS = Pattern.compile("\\d{" + MIN_DIGITS + "," + MAX_DIGITS + "}");

	private PhoneNumbers() {
	}

	/** Quita "+", espacios, guiones y paréntesis; valida que queden entre 8 y 15 dígitos. */
	public static String normalize(String rawPhone) {
		if (rawPhone == null || rawPhone.isBlank()) {
			throw new InvalidFieldException("phone", "must not be null or blank");
		}
		String withoutPlus = rawPhone.strip().startsWith("+") ? rawPhone.strip().substring(1) : rawPhone.strip();
		String digits = SEPARATORS.matcher(withoutPlus).replaceAll("");
		if (!DIGITS.matcher(digits).matches()) {
			throw new InvalidFieldException("phone",
					"must contain between " + MIN_DIGITS + " and " + MAX_DIGITS + " digits in international format");
		}
		return digits;
	}
}
