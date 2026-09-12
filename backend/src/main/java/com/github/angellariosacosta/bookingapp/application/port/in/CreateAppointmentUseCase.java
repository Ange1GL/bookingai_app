package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.CreateAppointmentCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

public interface CreateAppointmentUseCase {
	
	Appointment create(CreateAppointmentCommand createAppointmentCommand);
}
