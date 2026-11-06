
package com.hardwarestore.security;

import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetOtpService otpService;

    private static final String MOBILE_SESSION = "passwordResetMobile";
    private static final String VERIFIED_SESSION = "passwordResetVerified";

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("forgotForm", new ForgotForm());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestOtp(
            @ModelAttribute("forgotForm") ForgotForm form,
            HttpSession session,
            Model model) {

        try {
            if (form.getMobileNumber() == null
                    || form.getMobileNumber().isBlank()) {
                model.addAttribute("errorMessage", "Enter your mobile number.");
                return "forgot-password";
            }

            otpService.requestOtp(form.getMobileNumber());

            session.setAttribute(MOBILE_SESSION, form.getMobileNumber());
            session.removeAttribute(VERIFIED_SESSION);

            model.addAttribute(
                    "successMessage",
                    "If the mobile number belongs to an administrator, an OTP has been sent."
            );

            model.addAttribute("otpForm", new OtpForm());

            return "verify-otp";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "forgot-password";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtp(HttpSession session, Model model) {

        if (session.getAttribute(MOBILE_SESSION) == null) {
            return "redirect:/forgot-password";
        }

        model.addAttribute("otpForm", new OtpForm());
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @ModelAttribute("otpForm") OtpForm form,
            HttpSession session,
            Model model) {

        String mobileNumber = (String) session.getAttribute(MOBILE_SESSION);

        if (mobileNumber == null) {
            return "redirect:/forgot-password";
        }

        try {
            if (form.getOtp() == null
                    || !form.getOtp().matches("\\d{6}")) {
                throw new IllegalArgumentException(
                        "Enter the six-digit OTP."
                );
            }

            otpService.verifyOtp(mobileNumber, form.getOtp());

            session.setAttribute(VERIFIED_SESSION, true);

            return "redirect:/reset-password";

        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "verify-otp";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPassword(
            HttpSession session,
            Model model) {

        if (!Boolean.TRUE.equals(session.getAttribute(VERIFIED_SESSION))) {
            return "redirect:/forgot-password";
        }

        model.addAttribute("resetForm", new ResetForm());
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @ModelAttribute("resetForm") ResetForm form,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!Boolean.TRUE.equals(session.getAttribute(VERIFIED_SESSION))) {
            return "redirect:/forgot-password";
        }

        if (form.getNewPassword() == null
                || form.getNewPassword().length() < 8) {
            model.addAttribute(
                    "errorMessage",
                    "Password must contain at least 8 characters."
            );
            return "reset-password";
        }

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            model.addAttribute(
                    "errorMessage",
                    "Passwords do not match."
            );
            return "reset-password";
        }

        try {
            String mobileNumber =
                    (String) session.getAttribute(MOBILE_SESSION);

            otpService.completePasswordReset(
                    mobileNumber,
                    form.getNewPassword()
            );

            session.removeAttribute(MOBILE_SESSION);
            session.removeAttribute(VERIFIED_SESSION);

            redirectAttributes.addFlashAttribute(
                    "resetSuccess",
                    "Your password has been changed. Please log in."
            );

            return "redirect:/login";

        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "reset-password";
        }
    }

    @Getter
    @Setter
    public static class ForgotForm {
        private String mobileNumber;
    }

    @Getter
    @Setter
    public static class OtpForm {
        private String otp;
    }

    @Getter
    @Setter
    public static class ResetForm {
        private String newPassword;
        private String confirmPassword;
    }
}
