package com.example.OpShare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String name;
    private String contactNumber;
    private String token;
    private LocalDateTime tokenExpiresAt;
    private String deviceId;
    private boolean newUser;
    private String message;
}