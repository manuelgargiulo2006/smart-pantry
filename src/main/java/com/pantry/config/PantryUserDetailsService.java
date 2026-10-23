package com.pantry.config;

import com.pantry.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Spiega a Spring Security come caricare un utente dalla tabella users. */
@Service
public class PantryUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username.trim())
                .map(PantryUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }

    public PantryUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
