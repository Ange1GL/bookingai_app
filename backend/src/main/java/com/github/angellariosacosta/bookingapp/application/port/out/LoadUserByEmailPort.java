package com.github.angellariosacosta.bookingapp.application.port.out;

import com.github.angellariosacosta.bookingapp.domain.model.User;

public interface LoadUserByEmailPort {
    User loadByEmail(String email);
}
