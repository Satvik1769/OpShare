package com.example.OpShare.service;

import com.example.OpShare.dto.OtpResponse;
import com.example.OpShare.dto.SendOtpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final TwilioService twilioService;

    public OtpResponse sendOtp(SendOtpRequest request, String purpose) {
        String contactNumber = request.getContactNumber();
        log.info("Sending OTP for contact number: {} (purpose: {})", contactNumber, purpose);

        boolean sent = twilioService.sendOtp(contactNumber);
        if (!sent) {
            log.error("Failed to send OTP to: {}", contactNumber);
            throw new RuntimeException("Failed to send OTP. Please try again.");
        }

        return OtpResponse.builder()
                .contactNumber(contactNumber)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .remainingAttempts(5)
                .message("OTP sent successfully")
                .build();
    }

    public boolean verifyOtp(String contactNumber, String otpCode) {
        log.info("Verifying OTP for contact number: {}", contactNumber);

        contactNumber = "+91" + contactNumber;

        boolean verified = twilioService.verifyOtp(contactNumber, otpCode);
        if (!verified) {
            log.warn("Invalid OTP for contact number: {}", contactNumber);
            throw new RuntimeException("Invalid OTP. Please try again.");
        }

        log.info("OTP verified successfully for contact number: {}", contactNumber);
        return true;
    }
}