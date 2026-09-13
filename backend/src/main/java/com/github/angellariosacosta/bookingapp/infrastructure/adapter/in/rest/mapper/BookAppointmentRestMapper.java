package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.BookAppointmentCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AppointmentResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.BookAppointmentRequest;

@Component
public class BookAppointmentRestMapper {

	public BookAppointmentCommand toCommand(BookAppointmentRequest request, Long userId) {
		return new BookAppointmentCommand(
				request.name(),
				request.phone(),
				request.startTime(),
				request.endTime(),
				userId
		);
	}

	public AppointmentResponse toResponse(Appointment appointment) {
		return new AppointmentResponse(
				appointment.getId(),
				appointment.getStartTime(),
				appointment.getEndTime(),
				appointment.getCustomer().getName(),
				appointment.getStatus().getName(),
				appointment.getUserId()
		);
	}
}
