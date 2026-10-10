package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.RegisterNoShowCommand;
import com.github.angellariosacosta.bookingapp.application.result.RegisterNoShowResult;
import com.github.angellariosacosta.bookingapp.domain.model.NoShow;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.NoShowItemResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.NoShowResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.RegisterNoShowRequest;

@Component
public class NoShowRestMapper {

	// El cuerpo del POST /no-show es opcional: sin cuerpo, el motivo queda vacío.
	public RegisterNoShowCommand toCommand(Long appointmentId, RegisterNoShowRequest request, Long userId) {
		return new RegisterNoShowCommand(appointmentId, userId, request == null ? null : request.reason());
	}

	public NoShowResponse toResponse(RegisterNoShowResult result) {
		NoShow noShow = result.noShow();
		return new NoShowResponse(
				noShow.getId(),
				noShow.getAppointmentId(),
				noShow.getCustomerId(),
				noShow.getReason(),
				result.activeNoShows(),
				result.customerBlacklisted());
	}

	public List<NoShowItemResponse> toItemResponses(List<NoShow> noShows) {
		return noShows.stream()
				.map(noShow -> new NoShowItemResponse(
						noShow.getId(), noShow.getAppointmentId(), noShow.getReason(), noShow.getCreatedAt()))
				.toList();
	}
}
