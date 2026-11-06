
package com.hardwarestore.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(
        name = "app.otp.delivery-mode",
        havingValue = "console"
)
public class ConsoleOtpDeliveryService implements OtpDeliveryService {

    @Override
    public void sendOtp(String mobileNumber, String otp) {

        log.warn(
                "DEVELOPMENT ONLY - Password reset OTP for {} is {}",
                mobileNumber,
                otp
        );
    }
}
