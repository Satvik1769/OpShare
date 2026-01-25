package com.example.OpShare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferStatsResponse {
    private Long totalTransfers;
    private Long pendingCount;
    private Long inProgressCount;
    private Long completedCount;
    private Long abortedCount;
    private Long failedCount;
    private Long sentCount;
    private Long receivedCount;
    private Long activeCount;
}