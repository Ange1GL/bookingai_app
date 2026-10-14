package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.command.ReceiveWhatsAppEventCommand;

public interface ReceiveWhatsAppEventUseCase {
	void receive(ReceiveWhatsAppEventCommand command);
}
