package com.example.OpShare.service;

import com.example.OpShare.dto.*;
import com.example.OpShare.entity.Peer;
import com.example.OpShare.entity.PeerDeviceLogin;
import com.example.OpShare.repository.peerDeviceLoginRepository;
import com.example.OpShare.repository.peerRepository;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final peerRepository peerRepository;
    private final peerDeviceLoginRepository peerDeviceLoginRepository;
    private final OtpService otpService;
    private final JwtService jwtService;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_LOGGED_OUT = "LOGGED_OUT";

    @Transactional
    public OtpResponse sendLoginOtp(SendOtpRequest request) {
        log.error("Sending login OTP for contact: {}", request.getContactNumber());
        return otpService.sendOtp(request, "LOGIN");
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(VerifyOtpRequest request, HttpServletRequest httpServletRequest) {
        log.error("Verifying OTP and logging in for contact: {}", request.getContactNumber());

        // Verify OTP
        otpService.verifyOtp(request.getContactNumber(), request.getOtpCode());

        // Check if peer exists
        Optional<Peer> existingPeer = peerRepository.findByContactNumber(request.getContactNumber());
        boolean isNewUser = existingPeer.isEmpty();

        String ipAddress = getIpFromRequest(httpServletRequest);

        Peer peer;
        if (isNewUser) {
            // Create new peer
            peer = Peer.builder()
                    .contactNumber(request.getContactNumber())
                    .name("User_" + request.getContactNumber().substring(Math.max(0, request.getContactNumber().length() - 4)))
                    .verified(true)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .lastSeen(LocalDateTime.now())
                    .ip(ipAddress)
                    .build();
            peer = peerRepository.save(peer);
            log.error("Created new peer with ID: {}", peer.getId());
        } else {
            peer = existingPeer.get();
            peer.setVerified(true);
            peer.setIp(ipAddress);
            peer.setActive(true);
            peer.setLastSeen(LocalDateTime.now());
            peer.setUpdatedAt(LocalDateTime.now());
            peerRepository.save(peer);
        }

        // Generate JWT token
        String token = jwtService.generateToken(peer.getId(), peer.getContactNumber(), request.getDeviceId());
        LocalDateTime tokenExpiresAt = jwtService.getExpirationFromToken(token);

        // Create or update device login
        PeerDeviceLogin deviceLogin = peerDeviceLoginRepository
                .findByUserIdAndDeviceId(peer.getId(), request.getDeviceId())
                .orElse(PeerDeviceLogin.builder()
                        .userId(peer.getId())
                        .deviceId(request.getDeviceId())
                        .createdAt(ZonedDateTime.now())
                        .build());

        deviceLogin.setAuthKey(token);
        deviceLogin.setDeviceName(request.getDeviceName());
        deviceLogin.setDeviceType(request.getDeviceType());
        deviceLogin.setStatus(STATUS_ACTIVE);
        deviceLogin.setLastLogin(ZonedDateTime.now());
        peerDeviceLoginRepository.save(deviceLogin);

        log.error("Login successful for peer ID: {} on device: {}", peer.getId(), request.getDeviceId());

        return AuthResponse.builder()
                .userId(peer.getId())
                .name(peer.getName())
                .contactNumber(peer.getContactNumber())
                .token(token)
                .tokenExpiresAt(tokenExpiresAt)
                .deviceId(request.getDeviceId())
                .newUser(isNewUser)
                .message(isNewUser ? "Registration and login successful" : "Login successful")
                .build();
    }

    @Transactional
    public void logout(String token) {
        log.error("Logging out user");

        if (!jwtService.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        Long userId = jwtService.getUserIdFromToken(token);
        String deviceId = jwtService.getDeviceIdFromToken(token);

        Optional<PeerDeviceLogin> deviceLogin = peerDeviceLoginRepository.findByUserIdAndDeviceId(userId, deviceId);
        if (deviceLogin.isPresent()) {
            PeerDeviceLogin login = deviceLogin.get();
            login.setStatus(STATUS_LOGGED_OUT);
            login.setAuthKey(null);
            peerDeviceLoginRepository.save(login);
        }

        deactivatePeerIfNoActiveDevices(userId);
        log.error("Logout successful for user ID: {}", userId);
    }

    @Transactional
    public void logoutAllDevices(Long userId) {
        log.error("Logging out all devices for user ID: {}", userId);

        List<PeerDeviceLogin> devices = peerDeviceLoginRepository.findByUserId(userId);
        devices.forEach(device -> {
            device.setStatus(STATUS_LOGGED_OUT);
            device.setAuthKey(null);
            peerDeviceLoginRepository.save(device);
        });

        deactivatePeer(userId);
        log.error("Logged out {} devices for user ID: {}", devices.size(), userId);
    }

    public List<DeviceLoginResponse> getActiveDevices(Long userId) {
        return peerDeviceLoginRepository.findByUserIdAndStatus(userId, STATUS_ACTIVE)
                .stream()
                .map(this::toDeviceLoginResponse)
                .collect(Collectors.toList());
    }

    public List<DeviceLoginResponse> getAllDevices(Long userId) {
        return peerDeviceLoginRepository.findByUserId(userId)
                .stream()
                .map(this::toDeviceLoginResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void logoutDevice(Long userId, Long deviceLoginId) {
        PeerDeviceLogin device = peerDeviceLoginRepository.findById(deviceLoginId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (!device.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to logout this device");
        }

        device.setStatus(STATUS_LOGGED_OUT);
        device.setAuthKey(null);
        peerDeviceLoginRepository.save(device);

        deactivatePeerIfNoActiveDevices(userId);
        log.error("Logged out device ID: {} for user ID: {}", deviceLoginId, userId);
    }

    public boolean validateSession(String token) {
        if (!jwtService.validateToken(token)) {
            return false;
        }

        Long userId = jwtService.getUserIdFromToken(token);
        String deviceId = jwtService.getDeviceIdFromToken(token);

        Optional<PeerDeviceLogin> deviceLogin = peerDeviceLoginRepository.findByUserIdAndDeviceId(userId, deviceId);
        if (deviceLogin.isEmpty()) {
            return false;
        }

        PeerDeviceLogin login = deviceLogin.get();
        return STATUS_ACTIVE.equals(login.getStatus()) && token.equals(login.getAuthKey());
    }

    private void deactivatePeerIfNoActiveDevices(Long userId) {
        boolean hasActiveDevices = !peerDeviceLoginRepository.findByUserIdAndStatus(userId, STATUS_ACTIVE).isEmpty();
        if (!hasActiveDevices) {
            deactivatePeer(userId);
        }
    }

    private void deactivatePeer(Long userId) {
        peerRepository.findById(userId).ifPresent(peer -> {
            peer.setActive(false);
            peer.setUpdatedAt(LocalDateTime.now());
            peerRepository.save(peer);
            log.error("Peer {} marked inactive", userId);
        });
    }

    private DeviceLoginResponse toDeviceLoginResponse(PeerDeviceLogin device) {
        return DeviceLoginResponse.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .status(device.getStatus())
                .createdAt(device.getCreatedAt())
                .lastLogin(device.getLastLogin())
                .build();
    }

    private String getIpFromRequest(HttpServletRequest httpServletRequest) {
        String userIp = null;
        try{
            if (httpServletRequest != null && httpServletRequest.getHeader("X-FORWARDED-FOR") != null && !httpServletRequest.getHeader("X-FORWARDED-FOR").isEmpty()) {
                userIp = httpServletRequest.getHeader("X-FORWARDED-FOR");
                if (StringUtils.isNotEmpty(userIp) && userIp.split(",").length > 1) {
                    userIp = userIp.split(",")[0];
                }
            } else if(httpServletRequest != null) {
                userIp = httpServletRequest.getRemoteAddr();
            }
        }catch (Exception e){
            userIp = "192.168.1.1";
            log.error(e.getMessage(),e);
        }
        return userIp;
    }
}