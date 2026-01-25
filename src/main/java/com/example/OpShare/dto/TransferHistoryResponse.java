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
public class TransferHistoryResponse {
    private Long id;
    private Long roomId;
    private Long fileId;
    private String fileName;
    private BigInteger fileSize;
    private String contentType;
    private Long userId;
    private Long peerId;
    private String peerName;
    private Integer totalPeers;
    private String direction; // SENT, RECEIVED
    private String status; // PENDING, IN_PROGRESS, COMPLETED, ABORTED, FAILED
    private String errorReason;
    private Integer progressPercentage;
    private BigInteger bytesTransferred;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}