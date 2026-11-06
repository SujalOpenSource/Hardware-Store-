
package com.hardwarestore.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetOtpRepository
        extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp>
    findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(
            String mobileNumber
    );

    long countByMobileNumberAndCreatedAtAfter(
            String mobileNumber,
            LocalDateTime after
    );
}
