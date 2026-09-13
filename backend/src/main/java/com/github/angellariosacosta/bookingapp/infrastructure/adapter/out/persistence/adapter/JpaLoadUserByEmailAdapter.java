package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;



import com.github.angellariosacosta.bookingapp.application.port.out.LoadUserByEmailPort;
import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaLoadUserByEmailAdapter implements LoadUserByEmailPort {
    private final UserRepository userRepository;

    @Override
    public User loadByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }
}
