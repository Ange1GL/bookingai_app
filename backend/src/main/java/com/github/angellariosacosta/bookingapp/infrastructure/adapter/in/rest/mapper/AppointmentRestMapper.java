package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.CreateAppointmentCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.AppointmentResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.CreateAppointmentRequest;

@Component
public class AppointmentRestMapper {

	public CreateAppointmentCommand toCommand(CreateAppointmentRequest request, Long userId) {
		return new CreateAppointmentCommand(
				request.startTime(),
				request.endTime(),
				request.customerId(),
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
