package com.github.angellariosacosta.bookingapp.application.port.in;

public interface ChangeEmailUseCase {
    void changeEmail(Long userId, String newEmail);
}
