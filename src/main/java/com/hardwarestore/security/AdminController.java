
package com.hardwarestore.security;

import com.hardwarestore.security.registration.AdminRegistrationOtpService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private static final String REGISTRATION_MOBILE =
            "adminRegistrationMobile";

    private static final String REGISTRATION_VERIFIED =
            "adminRegistrationVerified";

    private final AdminRegistrationOtpService registrationOtpService;

    @GetMapping("/admin/register")
    public String showRegistrationPage(
            HttpSession session,
            Model model
    ) {
        model.addAttribute(
                "otpSent",
                session.getAttribute(REGISTRATION_MOBILE) != null
        );

        model.addAttribute(
                "otpVerified",
                Boolean.TRUE.equals(
                        session.getAttribute(REGISTRATION_VERIFIED)
                )
        );

        model.addAttribute(
                "mobileNumber",
                session.getAttribute(REGISTRATION_MOBILE)
        );

        return "admin/register";
    }

    @PostMapping("/admin/register/send-otp")
    public String sendRegistrationOtp(
            @RequestParam String mobileNumber,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            registrationOtpService.requestRegistrationOtp(mobileNumber);

            String normalizedNumber =
                    MobileNumberUtil.normalize(mobileNumber);

            session.setAttribute(REGISTRATION_MOBILE, normalizedNumber);
            session.removeAttribute(REGISTRATION_VERIFIED);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "OTP sent successfully. Please check your mobile phone."
            );

        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/admin/register";
    }

    @PostMapping("/admin/register/verify-otp")
    public String verifyRegistrationOtp(
            @RequestParam String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Object mobileObject = session.getAttribute(REGISTRATION_MOBILE);

        if (!(mobileObject instanceof String mobileNumber)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please request a new registration OTP first."
            );
            return "redirect:/admin/register";
        }

        if (otp == null || !otp.matches("\\d{6}")) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter a valid six-digit OTP."
            );
            return "redirect:/admin/register";
        }

        try {
            registrationOtpService.verifyRegistrationOtp(
                    mobileNumber,
                    otp
            );

            session.setAttribute(REGISTRATION_VERIFIED, true);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Mobile number verified. You can now create the administrator account."
            );

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/admin/register";
    }

    @PostMapping("/admin/register/create")
    public String createAdministrator(
            @RequestParam String password,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Object mobileObject = session.getAttribute(REGISTRATION_MOBILE);

        if (!(mobileObject instanceof String mobileNumber)
                || !Boolean.TRUE.equals(
                session.getAttribute(REGISTRATION_VERIFIED)
        )) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please verify the mobile number before creating an account."
            );

            return "redirect:/admin/register";
        }

        if (password == null || password.length() < 8) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Password must contain at least 8 characters."
            );

            return "redirect:/admin/register";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Password and confirm password do not match."
            );

            return "redirect:/admin/register";
        }

        try {
            registrationOtpService.completeRegistration(
                    mobileNumber,
                    password
            );

            session.removeAttribute(REGISTRATION_MOBILE);
            session.removeAttribute(REGISTRATION_VERIFIED);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Administrator registered successfully."
            );

            return "redirect:/admin/register";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/admin/register";
        }
    }

    @PostMapping("/admin/register/cancel")
    public String cancelRegistration(
            HttpSession session
    ) {
        session.removeAttribute(REGISTRATION_MOBILE);
        session.removeAttribute(REGISTRATION_VERIFIED);

        return "redirect:/admin/register";
    }
}
