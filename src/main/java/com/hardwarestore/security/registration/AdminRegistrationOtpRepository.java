
package com.hardwarestore.security.registration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AdminRegistrationOtpRepository
        extends JpaRepository<AdminRegistrationOtp, Long> {

    Optional<AdminRegistrationOtp>
    findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(
            String mobileNumber
    );

    long countByMobileNumberAndCreatedAtAfter(
            String mobileNumber,
            LocalDateTime dateTime
    );
}
