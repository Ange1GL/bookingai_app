package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LikePatternsTest {

	@Test
	void nullValueMatchesEverything() {
		assertEquals("%", LikePatterns.containsIgnoreCase(null));
		assertEquals("%", LikePatterns.contains(null));
	}

	@Test
	void nameIsLowercasedAndWrapped() {
		assertEquals("%juan pérez%", LikePatterns.containsIgnoreCase("Juan PÉREZ"));
	}

	@Test
	void phoneKeepsCaseAndIsWrapped() {
		assertEquals("%555%", LikePatterns.contains("555"));
	}

	@Test
	void wildcardsAndEscapeCharAreEscaped() {
		assertEquals("%100\\%\\_a\\\\b%", LikePatterns.contains("100%_a\\b"));
	}
}
