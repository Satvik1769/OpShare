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
public class FileEvent {
    private String eventType;
    private Long fileId;
    private String fileName;
    private BigInteger fileSize;
    private Long roomId;
    private Long fromUserId;
    private Long toUserId;
    private LocalDateTime timestamp;
}