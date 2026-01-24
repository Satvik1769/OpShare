package com.example.OpShare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {
    private String contactNumber;
    private String otpCode;
    private String deviceId;
    private String deviceName;
    private String deviceType;
}