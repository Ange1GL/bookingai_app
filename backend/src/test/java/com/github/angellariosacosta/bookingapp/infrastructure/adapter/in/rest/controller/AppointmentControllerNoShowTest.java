package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

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

import com.github.angellariosacosta.bookingapp.application.command.RegisterNoShowCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.QueryAppointmentsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.RegisterNoShowUseCase;
import com.github.angellariosacosta.bookingapp.application.result.RegisterNoShowResult;
import com.github.angellariosacosta.bookingapp.domain.exception.NoShowAlreadyRegisteredException;
import com.github.angellariosacosta.bookingapp.domain.model.NoShow;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.AppointmentRestMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.NoShowRestMapper;

class AppointmentControllerNoShowTest {

	private static final Long USER_ID = 7L;

	private final RegisterNoShowUseCase registerNoShow = mock(RegisterNoShowUseCase.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		AppointmentController controller = new AppointmentController(
				mock(CreateAppointmentUseCase.class), mock(QueryAppointmentsUseCase.class), registerNoShow,
				mock(AppointmentRestMapper.class), new NoShowRestMapper());
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setCustomArgumentResolvers(new FixedUserIdResolver())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void passesReasonAndTenantAndReturnsResult() throws Exception {
		NoShow saved = NoShow.reconstitute(1L, 5L, 40L, USER_ID, "no llego", Instant.parse("2026-10-09T12:00:00Z"));
		when(registerNoShow.register(any())).thenReturn(new RegisterNoShowResult(saved, 3, true));

		mockMvc.perform(post("/api/v1/appointments/40/no-show")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"no llego\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reason").value("no llego"))
				.andExpect(jsonPath("$.activeNoShows").value(3))
				.andExpect(jsonPath("$.customerBlacklisted").value(true));

		ArgumentCaptor<RegisterNoShowCommand> captor = ArgumentCaptor.forClass(RegisterNoShowCommand.class);
		verify(registerNoShow).register(captor.capture());
		assertEquals(new RegisterNoShowCommand(40L, USER_ID, "no llego"), captor.getValue());
	}

	@Test
	void reasonIsOptional() throws Exception {
		NoShow saved = NoShow.reconstitute(1L, 5L, 40L, USER_ID, null, Instant.parse("2026-10-09T12:00:00Z"));
		when(registerNoShow.register(any())).thenReturn(new RegisterNoShowResult(saved, 1, false));

		mockMvc.perform(post("/api/v1/appointments/40/no-show"))
				.andExpect(status().isOk());

		ArgumentCaptor<RegisterNoShowCommand> captor = ArgumentCaptor.forClass(RegisterNoShowCommand.class);
		verify(registerNoShow).register(captor.capture());
		assertEquals(new RegisterNoShowCommand(40L, USER_ID, null), captor.getValue());
	}

	@Test
	void acceptsReasonOfExactlyTwoHundredFiftyCharactersAndRejectsMore() throws Exception {
		NoShow saved = NoShow.reconstitute(1L, 5L, 40L, USER_ID, null, Instant.parse("2026-10-09T12:00:00Z"));
		when(registerNoShow.register(any())).thenReturn(new RegisterNoShowResult(saved, 1, false));

		mockMvc.perform(post("/api/v1/appointments/40/no-show")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"" + "x".repeat(250) + "\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/appointments/41/no-show")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"" + "x".repeat(251) + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void duplicateNoShowIsConflict() throws Exception {
		when(registerNoShow.register(any())).thenThrow(new NoShowAlreadyRegisteredException("dup"));

		mockMvc.perform(post("/api/v1/appointments/40/no-show"))
				.andExpect(status().isConflict());
	}

	@Test
	void doesNotCallUseCaseWhenBodyIsInvalid() throws Exception {
		mockMvc.perform(post("/api/v1/appointments/40/no-show")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"" + "x".repeat(300) + "\"}"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(registerNoShow);
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
