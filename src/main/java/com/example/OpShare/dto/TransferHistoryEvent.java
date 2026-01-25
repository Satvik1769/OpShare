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
public class TransferHistoryEvent {
    private String eventType; // TRANSFER_STARTED, TRANSFER_PROGRESS, TRANSFER_COMPLETED, TRANSFER_ABORTED, TRANSFER_FAILED
    private Long historyId;
    private Long roomId;
    private Long fileId;
    private String fileName;
    private BigInteger fileSize;
    private Long userId;
    private Long peerId;
    private String direction;
    private String status;
    private Integer progressPercentage;
    private BigInteger bytesTransferred;
    private String errorReason;
    private LocalDateTime timestamp;
}