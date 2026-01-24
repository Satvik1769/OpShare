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
public class FileOfferResponse {
    private Long fileId;
    private String fileName;
    private BigInteger fileSize;
    private String contentType;
    private Long offeredBy;
    private String status;
    private LocalDateTime offeredAt;
    private String message;
}