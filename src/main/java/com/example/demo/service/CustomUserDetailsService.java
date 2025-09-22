// src/main/java/com/example/demo/service/CustomUserDetailsService.java - ROZSZERZONY
package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika: " + username));

        // NOWE - Sprawdź czy konto jest aktywne
        if (!user.isActive()) {
            throw new RuntimeException("Konto użytkownika jest nieaktywne");
        }

        // NOWE - Dodaj authorities na podstawie roli systemowej
        Collection<GrantedAuthority> authorities = getAuthorities(user);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }

    // NOWA METODA - Pobieranie uprawnień na podstawie roli systemowej
    private Collection<GrantedAuthority> getAuthorities(User user) {
        switch (user.getSystemRole()) {
            case SUPER_ADMIN:
                return Collections.singletonList(new SimpleGrantedAuthority("SUPER_ADMIN"));
            case USER:
            default:
                return Collections.singletonList(new SimpleGrantedAuthority("USER"));
        }
    }
}