package com.github.angellariosacosta.bookingapp.infrastructure.config;

import com.github.angellariosacosta.bookingapp.application.port.out.LoadUserByEmailPort;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final LoadUserByEmailPort loadUserByEmailPort;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            User user = loadUserByEmailPort.loadByEmail(email);
            return new CustomUserDetails(user);
        } catch (Exception ex) {
            throw new UsernameNotFoundException("User not found: " + email, ex);
        }
    }
}
