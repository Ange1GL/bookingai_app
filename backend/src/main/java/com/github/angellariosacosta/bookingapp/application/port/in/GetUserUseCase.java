package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.domain.model.User;

public interface GetUserUseCase {

	User get(Long id);
}
