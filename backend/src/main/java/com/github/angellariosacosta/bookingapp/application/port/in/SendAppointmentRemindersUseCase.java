package com.github.angellariosacosta.bookingapp.application.port.in;

public interface SendAppointmentRemindersUseCase {

	/** Avisa de las citas que están por empezar y aún no se avisaron; devuelve cuántas citas se procesaron. */
	int sendDueReminders();
}
