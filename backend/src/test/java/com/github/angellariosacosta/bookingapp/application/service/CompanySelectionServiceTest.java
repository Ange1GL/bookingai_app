package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.port.out.CompanyRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerConversationPort;
import com.github.angellariosacosta.bookingapp.application.result.CompanySelection;
import com.github.angellariosacosta.bookingapp.domain.exception.CompanyNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Company;

@ExtendWith(MockitoExtension.class)
class CompanySelectionServiceTest {

	private static final String KEY = "1001:5215512345678";
	private static final Company COMPANY = new Company(10L, "Barbería El Güero", 7L);

	@Mock
	private CompanyRepositoryPort companyRepository;

	@Mock
	private CustomerConversationPort conversations;

	@InjectMocks
	private CompanySelectionService service;

	@DisplayName("Should store the company with its owner as the conversation tenant")
	@Test
	void shouldStoreSelectionWithOwner() {
		when(companyRepository.findById(10L)).thenReturn(Optional.of(COMPANY));

		CompanySelection selection = service.select(KEY, 10L);

		CompanySelection expected = new CompanySelection(10L, "Barbería El Güero", 7L);
		assertEquals(expected, selection);
		verify(conversations).saveSelection(KEY, expected);
	}

	@DisplayName("Should not store anything when the company does not exist")
	@Test
	void shouldNotStoreWhenCompanyDoesNotExist() {
		when(companyRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(CompanyNotFoundException.class, () -> service.select(KEY, 99L));

		verify(conversations, never()).saveSelection(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@DisplayName("Should return the current selection of the conversation")
	@Test
	void shouldReturnCurrentSelection() {
		CompanySelection stored = new CompanySelection(10L, "Barbería El Güero", 7L);
		when(conversations.findSelection(KEY)).thenReturn(Optional.of(stored));

		assertEquals(Optional.of(stored), service.current(KEY));
	}
}
