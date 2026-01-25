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
public class InitUploadResponse {
    private String uploadId;
    private Long roomId;
    private String fileName;
    private BigInteger fileSize;
    private Integer chunkSize;
    private Integer totalChunks;
    private boolean deduplicated;
    private Long existingFileId;
    private LocalDateTime expiresAt;
    private String message;
}