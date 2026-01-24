package com.example.OpShare.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "peer_device_login")
public class PeerDeviceLogin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "auth_key")
    private String authKey;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "status")
    private String status;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "last_login")
    private ZonedDateTime lastLogin;

    @Column(name = "device_type")
    private String deviceType;
}

