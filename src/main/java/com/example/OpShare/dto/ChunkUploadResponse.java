package com.example.OpShare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadResponse {
    private String uploadId;
    private Integer chunkNumber;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private BigInteger uploadedBytes;
    private BigInteger totalBytes;
    private Double progressPercent;
    private String status;
    private String message;
}