package com.github.angellariosacosta.bookingapp.application.port.out;



import com.github.angellariosacosta.bookingapp.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    User save(User user);
    boolean existsByEmail(String email);
}
