package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.RescheduleAppointmentCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

public interface RescheduleAppointmentUseCase {
	Appointment reschedule(RescheduleAppointmentCommand command);
}
