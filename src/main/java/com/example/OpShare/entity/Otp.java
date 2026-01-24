package com.example.OpShare.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "otp")
public class Otp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "verified")
    private boolean verified;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "purpose")
    private String purpose;
}