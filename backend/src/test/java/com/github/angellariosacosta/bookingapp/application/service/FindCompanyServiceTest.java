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
import com.github.angellariosacosta.bookingapp.domain.exception.CompanyNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Company;

@ExtendWith(MockitoExtension.class)
class FindCompanyServiceTest {

	private static final Long USER_ID = 1L;
	private static final Company COMPANY = new Company(10L, "Barbería El Güero", USER_ID);

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

	@DisplayName("Should search by the stripped name")
	@Test
	void shouldSearchByStrippedName() {
		when(companyRepository.searchByName("güero")).thenReturn(List.of(COMPANY));

		assertEquals(List.of(COMPANY), service.searchByName("  güero "));
		verify(companyRepository).searchByName("güero");
	}

	@DisplayName("Should return an empty list without querying when the name is blank")
	@Test
	void shouldReturnEmptyWhenNameIsBlank() {
		assertTrue(service.searchByName("  ").isEmpty());
		assertTrue(service.searchByName(null).isEmpty());

		verifyNoInteractions(companyRepository);
	}
}
