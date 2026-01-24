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
public class OtpResponse {
    private String contactNumber;
    private LocalDateTime expiresAt;
    private int remainingAttempts;
    private String message;
}