package com.github.angellariosacosta.bookingapp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.angellariosacosta.bookingapp.application.port.out.CustomerRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.query.CustomerSortField;
import com.github.angellariosacosta.bookingapp.application.query.ListCustomersQuery;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;

class ListCustomersServiceTest {

	private static final Long USER_ID = 7L;

	private final CustomerRepositoryPort repository = mock(CustomerRepositoryPort.class);
	private final ListCustomersService service = new ListCustomersService(repository);

	@Test
	void blankFiltersAreTreatedAsAbsent() {
		PageResult<Customer> expected = new PageResult<>(List.of(), 0, 20, 0, 0);
		when(repository.findPage(any())).thenReturn(expected);

		PageResult<Customer> result = service.list(query("   ", ""));

		assertSame(expected, result);
		ListCustomersQuery sent = capturedQuery();
		assertNull(sent.name());
		assertNull(sent.phone());
	}

	@Test
	void filtersAreStrippedAndTenantIsPreserved() {
		when(repository.findPage(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

		service.list(query("  Ana ", " 555 "));

		ListCustomersQuery sent = capturedQuery();
		assertEquals("Ana", sent.name());
		assertEquals("555", sent.phone());
		assertEquals(USER_ID, sent.userId());
	}

	private ListCustomersQuery capturedQuery() {
		ArgumentCaptor<ListCustomersQuery> captor = ArgumentCaptor.forClass(ListCustomersQuery.class);
		verify(repository).findPage(captor.capture());
		return captor.getValue();
	}

	private ListCustomersQuery query(String name, String phone) {
		return new ListCustomersQuery(USER_ID, name, phone, 0, 20, CustomerSortField.NAME, SortDirection.ASC);
	}
}
