package com.hardwarestore.security;

public final class MobileNumberUtil {

    private MobileNumberUtil() {
    }

    public static String normalize(String mobileNumber) {

        if (mobileNumber == null || mobileNumber.isBlank()) {
            throw new IllegalArgumentException("Mobile number is required.");
        }

        String number = mobileNumber.replaceAll("[\\s-]", "");

        if (number.startsWith("+977")) {
            number = number.substring(4);
        } else if (number.startsWith("977")) {
            number = number.substring(3);
        }

        if (!number.matches("9[78]\\d{8}")) {
            throw new IllegalArgumentException(
                    "Enter a valid Nepalese mobile number."
            );
        }

        return "+977" + number;
    }
}