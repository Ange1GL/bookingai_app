package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.memory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerConversationPort;
import com.github.angellariosacosta.bookingapp.application.result.CompanySelection;

/**
 * Estado en memoria (demo): se pierde al reiniciar y, con varias instancias, no se comparte.
 * Acotado a las conversaciones más recientes para no crecer sin límite.
 */
@Component
public class InMemoryCustomerConversationAdapter implements CustomerConversationPort {

	private static final int MAX_CONVERSATIONS = 1000;
	private static final float LOAD_FACTOR = 0.75f;

	private final Map<String, CompanySelection> selections = Collections.synchronizedMap(
			new LinkedHashMap<>(16, LOAD_FACTOR, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, CompanySelection> eldest) {
					return size() > MAX_CONVERSATIONS;
				}
			});

	@Override
	public Optional<CompanySelection> findSelection(String conversationKey) {
		return Optional.ofNullable(selections.get(conversationKey));
	}

	@Override
	public void saveSelection(String conversationKey, CompanySelection selection) {
		selections.put(conversationKey, selection);
	}
}
