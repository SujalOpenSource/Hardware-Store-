
        package com.hardwarestore.security;

import com.hardwarestore.security.AdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String initialUsername;

    @Value("${app.admin.password}")
    private String initialPassword;

    public AdminService(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Admin admin = adminRepository.findByUsername(initialUsername)
                .orElseGet(Admin::new);

        admin.setUsername(initialUsername);
        admin.setPassword(passwordEncoder.encode(initialPassword));
        admin.setEnabled(true);

        adminRepository.save(admin);
    }
}
