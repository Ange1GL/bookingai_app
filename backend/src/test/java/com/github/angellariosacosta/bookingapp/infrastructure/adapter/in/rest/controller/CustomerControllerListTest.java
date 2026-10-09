package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.github.angellariosacosta.bookingapp.application.port.in.CreateCustomerUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListCustomersUseCase;
import com.github.angellariosacosta.bookingapp.application.query.CustomerSortField;
import com.github.angellariosacosta.bookingapp.application.query.ListCustomersQuery;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.CustomerRestMapper;

class CustomerControllerListTest {

	private static final Long USER_ID = 7L;

	private final ListCustomersUseCase listCustomers = mock(ListCustomersUseCase.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		CustomerController controller = new CustomerController(
				mock(CreateCustomerUseCase.class), listCustomers, new CustomerRestMapper());
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setCustomArgumentResolvers(new FixedUserIdResolver())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void returnsPageWithDefaultsAndTenantFromAuthenticatedUser() throws Exception {
		Customer ana = Customer.builder().id(1L).name("Ana").phone("555").userId(USER_ID).build();
		when(listCustomers.list(any())).thenReturn(new PageResult<>(List.of(ana), 0, 20, 1, 1));

		mockMvc.perform(get("/api/v1/customers").param("name", "an"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Ana"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));

		ArgumentCaptor<ListCustomersQuery> captor = ArgumentCaptor.forClass(ListCustomersQuery.class);
		verify(listCustomers).list(captor.capture());
		ListCustomersQuery query = captor.getValue();
		assertEquals(USER_ID, query.userId());
		assertEquals("an", query.name());
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
