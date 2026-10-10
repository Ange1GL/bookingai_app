package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.Locale;

/** Construye patrones {@code LIKE ... ESCAPE '\'} seguros a partir de texto escrito por el usuario. */
final class LikePatterns {

	private static final String MATCH_ALL = "%";
	private static final char ESCAPE_CHAR = '\\';

	private LikePatterns() {
	}

	static String containsIgnoreCase(String value) {
		return value == null ? MATCH_ALL : MATCH_ALL + escape(value.toLowerCase(Locale.ROOT)) + MATCH_ALL;
	}

	static String contains(String value) {
		return value == null ? MATCH_ALL : MATCH_ALL + escape(value) + MATCH_ALL;
	}

	private static String escape(String value) {
		StringBuilder escaped = new StringBuilder(value.length());
		for (char c : value.toCharArray()) {
			if (c == '%' || c == '_' || c == ESCAPE_CHAR) {
				escaped.append(ESCAPE_CHAR);
			}
			escaped.append(c);
		}
		return escaped.toString();
	}
}
