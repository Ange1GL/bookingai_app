package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.github.angellariosacosta.bookingapp.application.command.BlacklistCustomerCommand;
import com.github.angellariosacosta.bookingapp.application.command.RemoveCustomerFromBlacklistCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.BlacklistCustomerUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateCustomerUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListCustomerNoShowsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListCustomersUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.RemoveCustomerFromBlacklistUseCase;
import com.github.angellariosacosta.bookingapp.application.query.CustomerSortField;
import com.github.angellariosacosta.bookingapp.application.query.ListCustomersQuery;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.result.BlacklistResult;
import com.github.angellariosacosta.bookingapp.application.result.CustomerListItem;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerBlacklistedException;
import com.github.angellariosacosta.bookingapp.domain.exception.CustomerNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.BlacklistSource;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.domain.model.NoShow;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.CustomerRestMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.NoShowRestMapper;

class CustomerControllerListTest {

	private static final Long USER_ID = 7L;

	private final ListCustomersUseCase listCustomers = mock(ListCustomersUseCase.class);
	private final BlacklistCustomerUseCase blacklistCustomer = mock(BlacklistCustomerUseCase.class);
	private final RemoveCustomerFromBlacklistUseCase removeFromBlacklist = mock(RemoveCustomerFromBlacklistUseCase.class);
	private final ListCustomerNoShowsUseCase listNoShows = mock(ListCustomerNoShowsUseCase.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		CustomerController controller = new CustomerController(
				mock(CreateCustomerUseCase.class), listCustomers, blacklistCustomer, removeFromBlacklist,
				listNoShows, new CustomerRestMapper(), new NoShowRestMapper());
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setCustomArgumentResolvers(new FixedUserIdResolver())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void returnsPageWithDefaultsAndTenantFromAuthenticatedUser() throws Exception {
		Customer ana = Customer.builder().id(1L).name("Ana").phone("555").userId(USER_ID).build();
		when(listCustomers.list(any()))
				.thenReturn(new PageResult<>(List.of(new CustomerListItem(ana, true)), 0, 20, 1, 1));

		mockMvc.perform(get("/api/v1/customers").param("name", "an").param("blacklisted", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Ana"))
				.andExpect(jsonPath("$.content[0].blacklisted").value(true))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));

		ArgumentCaptor<ListCustomersQuery> captor = ArgumentCaptor.forClass(ListCustomersQuery.class);
		verify(listCustomers).list(captor.capture());
		ListCustomersQuery query = captor.getValue();
		assertEquals(USER_ID, query.userId());
		assertEquals("an", query.name());
		assertEquals(Boolean.TRUE, query.blacklisted());
		assertEquals(20, query.size());
		assertEquals(CustomerSortField.NAME, query.sortBy());
		assertEquals(SortDirection.ASC, query.direction());
	}

	@Test
	void rejectsSizeAboveMaximum() throws Exception {
		mockMvc.perform(get("/api/v1/customers").param("size", "51"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(listCustomers);
	}

	@Test
	void rejectsNegativePage() throws Exception {
		mockMvc.perform(get("/api/v1/customers").param("page", "-1"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(listCustomers);
	}

	@Test
	void rejectsUnknownSortField() throws Exception {
		mockMvc.perform(get("/api/v1/customers").param("sortBy", "password"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(listCustomers);
	}

	@Test
	void blacklistWithoutBodyUsesAuthenticatedTenant() throws Exception {
		when(blacklistCustomer.blacklist(any()))
				.thenReturn(new BlacklistResult(5L, null, BlacklistSource.MANUAL, 2));

		mockMvc.perform(put("/api/v1/customers/5/blacklist"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerId").value(5))
				.andExpect(jsonPath("$.source").value("MANUAL"))
				.andExpect(jsonPath("$.cancelledAppointments").value(2));

		ArgumentCaptor<BlacklistCustomerCommand> captor = ArgumentCaptor.forClass(BlacklistCustomerCommand.class);
		verify(blacklistCustomer).blacklist(captor.capture());
		assertEquals(new BlacklistCustomerCommand(5L, USER_ID, null), captor.getValue());
	}

	@Test
	void blacklistPassesReasonAndRejectsTooLongOne() throws Exception {
		when(blacklistCustomer.blacklist(any()))
				.thenReturn(new BlacklistResult(5L, "no llego", BlacklistSource.MANUAL, 0));

		mockMvc.perform(put("/api/v1/customers/5/blacklist")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"no llego\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reason").value("no llego"));

		mockMvc.perform(put("/api/v1/customers/5/blacklist")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"" + "x".repeat(256) + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void blacklistOfUnknownCustomerIsNotFound() throws Exception {
		when(blacklistCustomer.blacklist(any())).thenThrow(new CustomerNotFoundException("Customer not found with id: 9"));

		mockMvc.perform(put("/api/v1/customers/9/blacklist"))
				.andExpect(status().isNotFound());
	}

	@Test
	void removeFromBlacklistReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/v1/customers/5/blacklist"))
				.andExpect(status().isNoContent());

		verify(removeFromBlacklist).remove(new RemoveCustomerFromBlacklistCommand(5L, USER_ID));
	}

	@Test
	void removeFromBlacklistWhenNotListedIsNotFound() throws Exception {
		org.mockito.Mockito.doThrow(new CustomerNotFoundException("Customer 5 is not in the blacklist"))
				.when(removeFromBlacklist).remove(any());

		mockMvc.perform(delete("/api/v1/customers/5/blacklist"))
				.andExpect(status().isNotFound());
	}

	@Test
	void listsNoShowHistory() throws Exception {
		when(listNoShows.list(5L, USER_ID)).thenReturn(List.of(
				NoShow.reconstitute(1L, 5L, 100L, USER_ID, java.time.Instant.parse("2026-10-09T12:00:00Z"))));

		mockMvc.perform(get("/api/v1/customers/5/no-shows"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].appointmentId").value(100));
	}

	@Test
	void blacklistedExceptionIsConflict() throws Exception {
		when(listCustomers.list(any())).thenThrow(new CustomerBlacklistedException("Customer 1 is blacklisted"));

		mockMvc.perform(get("/api/v1/customers"))
				.andExpect(status().isConflict());
	}

	private static final class FixedUserIdResolver implements HandlerMethodArgumentResolver {
		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.hasParameterAnnotation(CurrentUserId.class);
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			return USER_ID;
		}
	}
}
