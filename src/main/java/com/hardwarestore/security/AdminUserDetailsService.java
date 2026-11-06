package com.hardwarestore.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {

        String mobileNumber;

        try {
            mobileNumber = MobileNumberUtil.normalize(username);
        } catch (IllegalArgumentException exception) {
            throw new UsernameNotFoundException("Administrator not found.");
        }

        Admin admin = adminRepository.findByUsername(mobileNumber)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Administrator not found.")
                );

        return User.builder()
                .username(admin.getUsername())
                .password(admin.getPassword())
                .roles("ADMIN")
                .disabled(!admin.isEnabled())
                .build();
    }
}