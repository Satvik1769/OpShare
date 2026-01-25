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
public class InitUploadRequest {
    private Long roomId;
    private String fileName;
    private BigInteger fileSize;
    private String contentType;
    private String fileHash;
    private Integer chunkSize;
}