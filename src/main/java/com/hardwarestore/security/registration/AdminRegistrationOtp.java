
package com.hardwarestore.security.registration;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_registration_otps")
@Getter
@Setter
@NoArgsConstructor
public class AdminRegistrationOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String mobileNumber;

    @Column(nullable = false, length = 100)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean used;
}
