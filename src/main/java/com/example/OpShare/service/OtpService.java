package com.example.OpShare.service;

import com.example.OpShare.dto.OtpResponse;
import com.example.OpShare.dto.SendOtpRequest;
import com.example.OpShare.entity.Otp;
import com.example.OpShare.repository.otpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final otpRepository otpRepository;
    private final SmsService smsService;

    @Value("${otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.max-attempts}")
    private int maxAttempts;

    @Value("${otp.length}")
    private int otpLength;

    private static final String PURPOSE_LOGIN = "LOGIN";
    private static final String PURPOSE_REGISTER = "REGISTER";

    @Transactional
    public OtpResponse sendOtp(SendOtpRequest request, String purpose) {
        String contactNumber = request.getContactNumber();
        log.info("Generating OTP for contact number: {}", contactNumber);

        // Invalidate any existing unused OTPs
        invalidateExistingOtps(contactNumber);

        // Generate new OTP
        String otpCode = generateOtp();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(otpExpirationMinutes);

        Otp otp = Otp.builder()
                .contactNumber(contactNumber)
                .otpCode(otpCode)
                .createdAt(now)
                .expiresAt(expiresAt)
                .verified(false)
                .attempts(0)
                .purpose(purpose)
                .build();

        otpRepository.save(otp);

        // Send OTP via TextBee SMS service
        boolean smsSent = smsService.sendOtp(contactNumber, otpCode);
        if (!smsSent) {
            log.error("Failed to send OTP SMS to: {}", contactNumber);
            throw new RuntimeException("Failed to send OTP. Please try again.");
        }

        return OtpResponse.builder()
                .contactNumber(contactNumber)
                .expiresAt(expiresAt)
                .remainingAttempts(maxAttempts)
                .message("OTP sent successfully")
                .build();
    }

    @Transactional
    public boolean verifyOtp(String contactNumber, String otpCode) {
        log.info("Verifying OTP for contact number: {}", contactNumber);

        Optional<Otp> otpOptional = otpRepository
                .findTopByContactNumberAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        contactNumber, LocalDateTime.now());

        if (otpOptional.isEmpty()) {
            log.warn("No valid OTP found for contact number: {}", contactNumber);
            throw new RuntimeException("OTP expired or not found. Please request a new OTP.");
        }

        Otp otp = otpOptional.get();

        // Check attempts
        if (otp.getAttempts() >= maxAttempts) {
            log.warn("Max attempts exceeded for contact number: {}", contactNumber);
            throw new RuntimeException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        // Increment attempts
        otp.setAttempts(otp.getAttempts() + 1);
        otpRepository.save(otp);

        // Verify OTP
        if (!otp.getOtpCode().equals(otpCode)) {
            int remainingAttempts = maxAttempts - otp.getAttempts();
            log.warn("Invalid OTP for contact number: {}. Remaining attempts: {}", contactNumber, remainingAttempts);
            throw new RuntimeException("Invalid OTP. " + remainingAttempts + " attempts remaining.");
        }

        // Mark as verified
        otp.setVerified(true);
        otpRepository.save(otp);

        log.info("OTP verified successfully for contact number: {}", contactNumber);
        return true;
    }

    public int getRemainingAttempts(String contactNumber) {
        Optional<Otp> otpOptional = otpRepository
                .findTopByContactNumberAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        contactNumber, LocalDateTime.now());

        if (otpOptional.isEmpty()) {
            return 0;
        }

        return maxAttempts - otpOptional.get().getAttempts();
    }

    private void invalidateExistingOtps(String contactNumber) {
        otpRepository.findByContactNumberAndVerifiedFalse(contactNumber)
                .forEach(otp -> {
                    otp.setVerified(true);
                    otpRepository.save(otp);
                });
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up expired OTPs");
    }
}