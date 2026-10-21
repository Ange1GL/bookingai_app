package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.domain.model.Role;

public interface GetRoleUseCase {

	Role get(Long id);
}
