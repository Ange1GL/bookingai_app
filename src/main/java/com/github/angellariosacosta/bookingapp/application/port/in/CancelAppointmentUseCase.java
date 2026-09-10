package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.CancelAppointmentCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

public interface CancelAppointmentUseCase {
	Appointment cancel(CancelAppointmentCommand command);
}
