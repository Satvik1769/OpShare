package com.example.OpShare.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    @Value("${textbee.api-url}")
    private String apiUrl;

    @Value("${textbee.device-id}")
    private String deviceId;

    @Value("${textbee.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public boolean sendOtp(String phoneNumber, String otpCode) {
        log.info("Sending OTP to phone number: {}", phoneNumber);

        try {
            String url = apiUrl + "/" + deviceId + "/send-sms";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);

            String message = "Your OpShare verification code is: " + otpCode + ". Valid for 5 minutes. Do not share this code.";

            Map<String, Object> requestBody = Map.of(
                    "recipients", List.of(phoneNumber),
                    "message", message
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP sent successfully to: {}", phoneNumber);
                return true;
            } else {
                log.error("Failed to send OTP. Status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending OTP to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}