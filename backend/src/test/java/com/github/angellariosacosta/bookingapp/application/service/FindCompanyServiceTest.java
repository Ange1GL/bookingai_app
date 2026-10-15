package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.port.out.CompanyRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.result.CompanySearchResult;
import com.github.angellariosacosta.bookingapp.domain.exception.CompanyNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Company;

@ExtendWith(MockitoExtension.class)
class FindCompanyServiceTest {

	private static final Long USER_ID = 1L;
	private static final Company COMPANY = new Company(USER_ID, "Barbería El Güero");
	private static final CompanySearchResult RESULT = new CompanySearchResult(USER_ID, "Barbería El Güero", "Juan Pérez");
	private static final CompanySearchResult SAME_OWNER_NAME = new CompanySearchResult(2L, "Corte Fino", "Juan Pérez");

	@Mock
	private CompanyRepositoryPort companyRepository;

	@InjectMocks
	private FindCompanyService service;

	@DisplayName("Should return the company of the owner")
	@Test
	void shouldReturnCompanyOfOwner() {
		when(companyRepository.findByUserId(USER_ID)).thenReturn(Optional.of(COMPANY));

		assertEquals(COMPANY, service.findByOwner(USER_ID));
	}

	@DisplayName("Should throw when the owner has no company")
	@Test
	void shouldThrowWhenOwnerHasNoCompany() {
		when(companyRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

		assertThrows(CompanyNotFoundException.class, () -> service.findByOwner(USER_ID));
	}

	@DisplayName("Should search companies by the stripped business name")
	@Test
	void shouldSearchByStrippedName() {
		when(companyRepository.searchByName("güero")).thenReturn(List.of(RESULT));

		assertEquals(List.of(RESULT), service.searchByName("  güero "));
		verify(companyRepository).searchByName("güero");
	}

	@DisplayName("Should search companies by the stripped owner name and keep every owner with the same name")
	@Test
	void shouldSearchByStrippedOwnerNameReturningAllMatches() {
		when(companyRepository.searchByOwnerName("juan")).thenReturn(List.of(RESULT, SAME_OWNER_NAME));

		assertEquals(List.of(RESULT, SAME_OWNER_NAME), service.searchByOwnerName(" juan"));
		verify(companyRepository).searchByOwnerName("juan");
	}

	@DisplayName("Should return an empty list without querying when the text is blank")
	@Test
	void shouldReturnEmptyWhenTextIsBlank() {
		assertTrue(service.searchByName("  ").isEmpty());
		assertTrue(service.searchByName(null).isEmpty());
		assertTrue(service.searchByOwnerName("  ").isEmpty());
		assertTrue(service.searchByOwnerName(null).isEmpty());

		verifyNoInteractions(companyRepository);
	}
}
