package com.example.OpShare.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioService {

    private final RestTemplate restTemplate;

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.verify-service-sid}")
    private String verifyServiceSid;

    private static final String TWILIO_VERIFY_URL = "https://verify.twilio.com/v2/Services";

    public boolean sendOtp(String phoneNumber) {
        log.error("Sending OTP to phone number: {}", phoneNumber);

        try {
            String url = TWILIO_VERIFY_URL + "/" + verifyServiceSid + "/Verifications";

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("To", phoneNumber);
            body.add("Channel", "sms");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map responseBody = response.getBody();
                String status = responseBody != null ? (String) responseBody.get("status") : null;
                log.error("OTP sent successfully to {}. Status: {}", phoneNumber, status);
                return "pending".equals(status);
            } else {
                log.error("Failed to send OTP. Status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending OTP to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }

    public boolean verifyOtp(String phoneNumber, String code) {
        log.error("Verifying OTP for phone number: {}", phoneNumber);

        try {
            String url = TWILIO_VERIFY_URL + "/" + verifyServiceSid + "/VerificationCheck";

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("To", phoneNumber);
            body.add("Code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map responseBody = response.getBody();
                String status = responseBody != null ? (String) responseBody.get("status") : null;
                log.error("OTP verification for {}. Status: {}", phoneNumber, status);
                return "approved".equals(status);
            } else {
                log.error("OTP verification failed. Status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error verifying OTP for {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = accountSid + ":" + authToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }
}