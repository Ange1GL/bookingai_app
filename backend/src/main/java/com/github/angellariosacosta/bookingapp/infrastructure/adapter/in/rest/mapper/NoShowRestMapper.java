package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.result.RegisterNoShowResult;
import com.github.angellariosacosta.bookingapp.domain.model.NoShow;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.NoShowItemResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.NoShowResponse;

@Component
public class NoShowRestMapper {

	public NoShowResponse toResponse(RegisterNoShowResult result) {
		NoShow noShow = result.noShow();
		return new NoShowResponse(
				noShow.getId(),
				noShow.getAppointmentId(),
				noShow.getCustomerId(),
				result.activeNoShows(),
				result.customerBlacklisted());
	}

	public List<NoShowItemResponse> toItemResponses(List<NoShow> noShows) {
		return noShows.stream()
				.map(noShow -> new NoShowItemResponse(noShow.getId(), noShow.getAppointmentId(), noShow.getCreatedAt()))
				.toList();
	}
}
