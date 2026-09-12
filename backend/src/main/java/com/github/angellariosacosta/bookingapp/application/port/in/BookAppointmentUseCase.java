package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.BookAppointmentCommand;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;

public interface BookAppointmentUseCase {
	Appointment book(BookAppointmentCommand command);
}
