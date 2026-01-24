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
public class FileUploadResponse {
    private Long fileId;
    private String fileName;
    private BigInteger fileSize;
    private String contentType;
    private String status;
    private Long roomId;
    private Long uploadedBy;
    private LocalDateTime createdAt;
    private String message;
}