package com.example.OpShare.controller;

import com.example.OpShare.dto.*;
import com.example.OpShare.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<OtpResponse> sendOtp(@RequestBody SendOtpRequest request) {
        log.error("Received OTP request for contact: {}", request.getContactNumber());
        OtpResponse response = authService.sendLoginOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody VerifyOtpRequest request, HttpServletRequest httpServletRequest) {
        log.error("Received OTP verification request for contact: {}", request.getContactNumber());
        AuthResponse response = authService.verifyOtpAndLogin(request, httpServletRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        log.error("Received logout request");
        String token = extractToken(authHeader);
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAllDevices(Principal principal) {
        log.error("Received logout all devices request");
        Long userId = Long.parseLong(principal.getName());
        authService.logoutAllDevices(userId);
        return ResponseEntity.ok(Map.of("message", "Logged out from all devices successfully"));
    }

    @GetMapping("/devices")
    public ResponseEntity<List<DeviceLoginResponse>> getActiveDevices(Principal principal) {
        log.error("Received get active devices request");
        Long userId = Long.parseLong(principal.getName());
        List<DeviceLoginResponse> devices = authService.getActiveDevices(userId);
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/devices/all")
    public ResponseEntity<List<DeviceLoginResponse>> getAllDevices(Principal principal) {
        log.error("Received get all devices request");
        Long userId = Long.parseLong(principal.getName());
        List<DeviceLoginResponse> devices = authService.getAllDevices(userId);
        return ResponseEntity.ok(devices);
    }

    @PostMapping("/devices/{deviceLoginId}/logout")
    public ResponseEntity<Map<String, String>> logoutDevice(
            @PathVariable Long deviceLoginId,
            Principal principal) {
        log.error("Received logout device request for device ID: {}", deviceLoginId);
        Long userId = Long.parseLong(principal.getName());
        authService.logoutDevice(userId, deviceLoginId);
        return ResponseEntity.ok(Map.of("message", "Device logged out successfully"));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSession(@RequestHeader("Authorization") String authHeader) {
        log.error("Received session validation request");
        String token = extractToken(authHeader);
        boolean isValid = authService.validateSession(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }
}