package com.hardwarestore.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.mobile-number:}")
    private String initialMobileNumber;

    @Value("${app.admin.password:}")
    private String initialPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (adminRepository.count() > 0) {
            log.info("Existing administrator account found. Bootstrap skipped.");
            return;
        }

        if (initialMobileNumber.isBlank() || initialPassword.isBlank()) {
            log.warn(
                    "No administrator exists. Configure ADMIN_MOBILE_NUMBER and ADMIN_INITIAL_PASSWORD."
            );
            return;
        }

        Admin admin = new Admin();
        admin.setUsername(MobileNumberUtil.normalize(initialMobileNumber));
        admin.setPassword(passwordEncoder.encode(initialPassword));
        admin.setEnabled(true);

        adminRepository.save(admin);

        log.info("Initial administrator account created.");
    }

    @Transactional
    public void registerAdmin(String mobileNumber, String password) {

        String normalizedNumber = MobileNumberUtil.normalize(mobileNumber);

        if (adminRepository.existsByUsername(normalizedNumber)) {
            throw new IllegalArgumentException(
                    "An administrator with this mobile number already exists."
            );
        }

        Admin admin = new Admin();
        admin.setUsername(normalizedNumber);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setEnabled(true);

        adminRepository.save(admin);
    }

    @Transactional
    public void updatePassword(String mobileNumber, String newPassword) {

        String normalizedNumber = MobileNumberUtil.normalize(mobileNumber);

        Admin admin = adminRepository.findByUsername(normalizedNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Administrator account not found."
                ));

        admin.setPassword(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);
    }
}