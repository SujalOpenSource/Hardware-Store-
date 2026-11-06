
package com.hardwarestore.security;

public interface OtpDeliveryService {

    void sendOtp(String mobileNumber, String otp);
}
