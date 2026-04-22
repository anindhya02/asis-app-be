package io.propenuy.asis_app_be.security.service;

import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) 
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));
        
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // harus sudah di-hash
                .authorities(Collections.singleton(authority))
                .disabled(!"active".equalsIgnoreCase(user.getStatus()))
                .build();
    }
}