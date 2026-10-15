package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.angellariosacosta.bookingapp.application.command.CreateCompanyCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.CompanyRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.CompanyAlreadyExistsException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.model.Company;

@ExtendWith(MockitoExtension.class)
class CreateCompanyServiceTest {

	private static final String NAME = "Barbería El Güero";
	private static final Long USER_ID = 1L;

	@Mock
	private CompanyRepositoryPort companyRepository;

	@InjectMocks
	private CreateCompanyService service;

	@DisplayName("Should save the company owned by the user when the user has none")
	@Test
	void shouldSaveCompanyOwnedByUser() {
		when(companyRepository.existsByUserId(USER_ID)).thenReturn(false);
		when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Company result = service.create(new CreateCompanyCommand(NAME, USER_ID));

		assertNull(result.getId());
		assertEquals(NAME, result.getName());
		assertEquals(USER_ID, result.getUserId());
	}

	@DisplayName("Should throw and not save when the user already has a company")
	@Test
	void shouldThrowWhenUserAlreadyHasCompany() {
		when(companyRepository.existsByUserId(USER_ID)).thenReturn(true);

		assertThrows(CompanyAlreadyExistsException.class,
				() -> service.create(new CreateCompanyCommand(NAME, USER_ID)));

		verify(companyRepository, never()).save(any(Company.class));
	}

	@DisplayName("Should fail before touching the repository when the name is blank")
	@Test
	void shouldFailBeforeTouchingRepositoryWhenNameIsBlank() {
		assertThrows(InvalidFieldException.class, () -> service.create(new CreateCompanyCommand(" ", USER_ID)));

		verifyNoInteractions(companyRepository);
	}
}
