package com.example.OpShare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceLoginResponse {
    private Long id;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String status;
    private ZonedDateTime createdAt;
    private ZonedDateTime lastLogin;
}