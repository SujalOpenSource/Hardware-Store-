
package com.hardwarestore.security.registration;

import com.hardwarestore.security.AdminRepository;
import com.hardwarestore.security.AdminService;
import com.hardwarestore.security.MobileNumberUtil;
import com.hardwarestore.security.OtpDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminRegistrationOtpService {

    private final AdminRegistrationOtpRepository otpRepository;
    private final AdminRepository adminRepository;
    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService otpDeliveryService;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate and send registration OTP.
     */
    @Transactional
    public void requestRegistrationOtp(String mobileNumber) {

        String normalizedNumber =
                MobileNumberUtil.normalize(mobileNumber);

        if (adminRepository.existsByUsername(normalizedNumber)) {
            throw new IllegalArgumentException(
                    "An administrator with this mobile number already exists."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        var previousOtp = otpRepository
                .findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(
                        normalizedNumber
                );

        if (previousOtp.isPresent()
                && previousOtp.get().getCreatedAt()
                .isAfter(now.minusSeconds(60))) {

            throw new IllegalArgumentException(
                    "Please wait 60 seconds before requesting another OTP."
            );
        }

        long requests = otpRepository
                .countByMobileNumberAndCreatedAtAfter(
                        normalizedNumber,
                        now.minusMinutes(15)
                );

        if (requests >= 3) {
            throw new IllegalArgumentException(
                    "Too many OTP requests. Please try again later."
            );
        }

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        AdminRegistrationOtp registrationOtp =
                new AdminRegistrationOtp();

        registrationOtp.setMobileNumber(normalizedNumber);
        registrationOtp.setOtpHash(passwordEncoder.encode(otp));
        registrationOtp.setCreatedAt(now);
        registrationOtp.setExpiresAt(now.plusMinutes(5));
        registrationOtp.setAttempts(0);
        registrationOtp.setUsed(false);

        otpRepository.save(registrationOtp);

        otpDeliveryService.sendOtp(normalizedNumber, otp);
    }

    /**
     * Verify registration OTP.
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verifyRegistrationOtp(
            String mobileNumber,
            String otp
    ) {

        String normalizedNumber =
                MobileNumberUtil.normalize(mobileNumber);

        AdminRegistrationOtp registrationOtp = otpRepository
                .findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(
                        normalizedNumber
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired registration OTP."
                ));

        if (registrationOtp.getVerifiedAt() != null) {
            throw new IllegalArgumentException(
                    "This OTP has already been verified."
            );
        }

        if (registrationOtp.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            registrationOtp.setUsed(true);
            otpRepository.save(registrationOtp);

            throw new IllegalArgumentException(
                    "OTP has expired. Request a new OTP."
            );
        }

        if (registrationOtp.getAttempts() >= 5) {

            registrationOtp.setUsed(true);
            otpRepository.save(registrationOtp);

            throw new IllegalArgumentException(
                    "Maximum OTP attempts exceeded. Request a new OTP."
            );
        }

        registrationOtp.setAttempts(
                registrationOtp.getAttempts() + 1
        );

        if (!passwordEncoder.matches(
                otp,
                registrationOtp.getOtpHash()
        )) {

            otpRepository.save(registrationOtp);

            throw new IllegalArgumentException(
                    "Incorrect OTP."
            );
        }

        registrationOtp.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(registrationOtp);
    }

    /**
     * Create administrator after successful OTP verification.
     */
    @Transactional
    public void completeRegistration(
            String mobileNumber,
            String password
    ) {

        String normalizedNumber =
                MobileNumberUtil.normalize(mobileNumber);

        AdminRegistrationOtp registrationOtp = otpRepository
                .findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(
                        normalizedNumber
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Please verify your mobile number first."
                ));

        LocalDateTime now = LocalDateTime.now();

        if (registrationOtp.getVerifiedAt() == null
                || registrationOtp.getVerifiedAt()
                .isBefore(now.minusMinutes(10))) {

            throw new IllegalArgumentException(
                    "OTP verification has expired. Please start again."
            );
        }

        if (registrationOtp.isUsed()) {
            throw new IllegalArgumentException(
                    "This registration request has already been used."
            );
        }

        if (adminRepository.existsByUsername(normalizedNumber)) {
            throw new IllegalArgumentException(
                    "An administrator with this mobile number already exists."
            );
        }

        adminService.registerAdmin(normalizedNumber, password);

        registrationOtp.setUsed(true);
        otpRepository.save(registrationOtp);
    }
}
