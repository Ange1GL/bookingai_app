package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerBlacklistRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.query.CustomerSortField;
import com.github.angellariosacosta.bookingapp.application.query.ListCustomersQuery;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.result.CustomerListItem;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;

class ListCustomersServiceTest {

	private static final Long USER_ID = 7L;

	private final CustomerRepositoryPort repository = mock(CustomerRepositoryPort.class);
	private final CustomerBlacklistRepositoryPort blacklistRepository = mock(CustomerBlacklistRepositoryPort.class);
	private final ListCustomersService service = new ListCustomersService(repository, blacklistRepository);

	@Test
	void blankFiltersAreTreatedAsAbsent() {
		when(repository.findPage(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
		when(blacklistRepository.findBlacklistedIds(any(), any())).thenReturn(Set.of());

		service.list(query("   ", ""));

		ListCustomersQuery sent = capturedQuery();
		assertNull(sent.name());
		assertNull(sent.phone());
	}

	@Test
	void filtersAreStrippedAndTenantIsPreserved() {
		when(repository.findPage(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
		when(blacklistRepository.findBlacklistedIds(any(), any())).thenReturn(Set.of());

		service.list(query("  Ana ", " 555 "));

		ListCustomersQuery sent = capturedQuery();
		assertEquals("Ana", sent.name());
		assertEquals("555", sent.phone());
		assertEquals(USER_ID, sent.userId());
	}

	@Test
	void marksBlacklistedCustomersOfThePage() {
		Customer ana = Customer.builder().id(1L).name("Ana").phone("555").userId(USER_ID).build();
		Customer luis = Customer.builder().id(2L).name("Luis").phone("777").userId(USER_ID).build();
		when(repository.findPage(any())).thenReturn(new PageResult<>(List.of(ana, luis), 0, 20, 2, 1));
		when(blacklistRepository.findBlacklistedIds(List.of(1L, 2L), USER_ID)).thenReturn(Set.of(2L));

		PageResult<CustomerListItem> result = service.list(query(null, null));

		assertFalse(result.content().get(0).blacklisted());
		assertTrue(result.content().get(1).blacklisted());
		assertEquals(2, result.totalElements());
	}

	private ListCustomersQuery capturedQuery() {
		ArgumentCaptor<ListCustomersQuery> captor = ArgumentCaptor.forClass(ListCustomersQuery.class);
		verify(repository).findPage(captor.capture());
		return captor.getValue();
	}

	private ListCustomersQuery query(String name, String phone) {
		return new ListCustomersQuery(USER_ID, name, phone, null, 0, 20, CustomerSortField.NAME, SortDirection.ASC);
	}
}
