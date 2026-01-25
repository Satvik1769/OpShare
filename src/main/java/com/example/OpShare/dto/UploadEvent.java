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
public class UploadEvent {
    private String eventType;
    private String uploadId;
    private Long fileId;
    private String fileName;
    private Long roomId;
    private Long userId;

    // Progress fields
    private BigInteger uploadedBytes;
    private BigInteger totalBytes;
    private Integer uploadedChunks;
    private Integer totalChunks;
    private Double progressPercent;

    // Status fields
    private String status;
    private String error;
    private LocalDateTime timestamp;

    // Event types
    public static final String UPLOAD_STARTED = "UPLOAD_STARTED";
    public static final String UPLOAD_PROGRESS = "UPLOAD_PROGRESS";
    public static final String UPLOAD_COMPLETED = "UPLOAD_COMPLETED";
    public static final String UPLOAD_FAILED = "UPLOAD_FAILED";
    public static final String UPLOAD_CANCELLED = "UPLOAD_CANCELLED";
    public static final String FILE_DEDUPLICATED = "FILE_DEDUPLICATED";
}