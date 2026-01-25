package com.example.OpShare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadProgressResponse {
    private String uploadId;
    private String fileName;
    private BigInteger fileSize;
    private BigInteger uploadedBytes;
    private BigInteger remainingBytes;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private Integer remainingChunks;
    private Double progressPercent;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}