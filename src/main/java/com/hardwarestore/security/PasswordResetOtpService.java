
package com.hardwarestore.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetOtpService {

    private final PasswordResetOtpRepository otpRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService otpDeliveryService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestOtp(String mobileNumber) {

        String normalizedNumber = MobileNumberUtil.normalize(mobileNumber);

        if (!adminRepository.existsByUsername(normalizedNumber)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        var previousOtp = otpRepository
                .findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(
                        normalizedNumber
                );

        if (previousOtp.isPresent()) {
            PasswordResetOtp previous = previousOtp.get();

            if (previous.getCreatedAt().isAfter(now.minusSeconds(60))) {
                throw new IllegalArgumentException(
                        "Please wait 60 seconds before requesting another OTP."
                );
            }
        }

        long requests = otpRepository.countByMobileNumberAndCreatedAtAfter(
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

        PasswordResetOtp resetOtp = new PasswordResetOtp();

        resetOtp.setMobileNumber(normalizedNumber);
        resetOtp.setOtpHash(passwordEncoder.encode(otp));
        resetOtp.setCreatedAt(now);
        resetOtp.setExpiresAt(now.plusMinutes(5));
        resetOtp.setAttempts(0);
        resetOtp.setUsed(false);

        otpRepository.save(resetOtp);

        otpDeliveryService.sendOtp(normalizedNumber, otp);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verifyOtp(String mobileNumber, String otp) {

        String normalizedNumber = MobileNumberUtil.normalize(mobileNumber);

        PasswordResetOtp resetOtp = otpRepository
                .findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(
                        normalizedNumber
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid or expired OTP.")
                );

        LocalDateTime now = LocalDateTime.now();

        if (resetOtp.getVerifiedAt() != null) {
            throw new IllegalArgumentException(
                    "This OTP has already been verified."
            );
        }

        if (!resetOtp.getExpiresAt().isAfter(now)) {
            resetOtp.setUsed(true);
            otpRepository.save(resetOtp);
            throw new IllegalArgumentException("OTP has expired.");
        }

        if (resetOtp.getAttempts() >= 5) {
            resetOtp.setUsed(true);
            otpRepository.save(resetOtp);
            throw new IllegalArgumentException(
                    "Maximum OTP attempts exceeded. Request a new OTP."
            );
        }

        resetOtp.setAttempts(resetOtp.getAttempts() + 1);

        if (!passwordEncoder.matches(otp, resetOtp.getOtpHash())) {
            otpRepository.save(resetOtp);
            throw new IllegalArgumentException("Incorrect OTP.");
        }

        resetOtp.setVerifiedAt(now);
        otpRepository.save(resetOtp);
    }

    @Transactional
    public void completePasswordReset(
            String mobileNumber,
            String newPassword
    ) {

        String normalizedNumber = MobileNumberUtil.normalize(mobileNumber);

        PasswordResetOtp resetOtp = otpRepository
                .findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(
                        normalizedNumber
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Please verify your OTP first."
                        )
                );

        LocalDateTime now = LocalDateTime.now();

        if (resetOtp.getVerifiedAt() == null
                || !resetOtp.getVerifiedAt().isAfter(now.minusMinutes(10))) {

            throw new IllegalArgumentException(
                    "Your OTP verification has expired. Please start again."
            );
        }

        if (resetOtp.isUsed()) {
            throw new IllegalArgumentException(
                    "This password reset request has already been used."
            );
        }

        Admin admin = adminRepository.findByUsername(normalizedNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("Administrator not found.")
                );

        admin.setPassword(passwordEncoder.encode(newPassword));
        resetOtp.setUsed(true);

        adminRepository.save(admin);
        otpRepository.save(resetOtp);
    }
}
