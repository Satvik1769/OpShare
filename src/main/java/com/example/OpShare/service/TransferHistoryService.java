package com.example.OpShare.service;

import com.example.OpShare.dto.TransferHistoryListResponse;
import com.example.OpShare.dto.TransferHistoryResponse;
import com.example.OpShare.dto.TransferStatsResponse;
import com.example.OpShare.entity.TransferHistory;
import com.example.OpShare.repository.TransferHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferHistoryService {

    private final TransferHistoryRepository transferHistoryRepository;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_ABORTED = "ABORTED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String DIRECTION_SENT = "SENT";
    public static final String DIRECTION_RECEIVED = "RECEIVED";

    /**
     * Create a new transfer history record
     */
    @Transactional
    public TransferHistory createTransferHistory(Long roomId, Long fileId, String fileName,
            BigInteger fileSize, String contentType, Long userId, Long peerId,
            String peerName, Integer totalPeers, String direction) {

        TransferHistory history = TransferHistory.builder()
                .roomId(roomId)
                .fileId(fileId)
                .fileName(fileName)
                .fileSize(fileSize)
                .contentType(contentType)
                .userId(userId)
                .peerId(peerId)
                .peerName(peerName)
                .totalPeers(totalPeers)
                .direction(direction)
                .status(STATUS_PENDING)
                .progressPercentage(0)
                .bytesTransferred(BigInteger.ZERO)
                .startedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return transferHistoryRepository.save(history);
    }

    /**
     * Update transfer status
     */
    @Transactional
    public TransferHistory updateStatus(Long historyId, String status, String errorReason) {
        TransferHistory history = transferHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Transfer history not found: " + historyId));

        history.setStatus(status);
        history.setUpdatedAt(LocalDateTime.now());

        if (errorReason != null) {
            history.setErrorReason(errorReason);
        }

        if (STATUS_COMPLETED.equals(status) || STATUS_ABORTED.equals(status) || STATUS_FAILED.equals(status)) {
            history.setCompletedAt(LocalDateTime.now());
        }

        if (STATUS_COMPLETED.equals(status)) {
            history.setProgressPercentage(100);
            history.setBytesTransferred(history.getFileSize());
        }

        return transferHistoryRepository.save(history);
    }

    /**
     * Update transfer progress
     */
    @Transactional
    public TransferHistory updateProgress(Long historyId, Integer progressPercentage, BigInteger bytesTransferred) {
        TransferHistory history = transferHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Transfer history not found: " + historyId));

        history.setProgressPercentage(progressPercentage);
        history.setBytesTransferred(bytesTransferred);
        history.setStatus(STATUS_IN_PROGRESS);
        history.setUpdatedAt(LocalDateTime.now());

        return transferHistoryRepository.save(history);
    }

    /**
     * Get all transfers for a user (paginated)
     */
    public TransferHistoryListResponse getAllTransfers(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferHistory> historyPage = transferHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return buildListResponse(historyPage);
    }

    /**
     * Get all transfers for a user (non-paginated)
     */
    public List<TransferHistoryResponse> getAllTransfers(Long userId) {
        List<TransferHistory> histories = transferHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return histories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Get transfers by status (paginated)
     */
    public TransferHistoryListResponse getTransfersByStatus(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferHistory> historyPage = transferHistoryRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);

        return buildListResponse(historyPage);
    }

    /**
     * Get active transfers (PENDING or IN_PROGRESS)
     */
    public List<TransferHistoryResponse> getActiveTransfers(Long userId) {
        List<TransferHistory> histories = transferHistoryRepository.findActiveTransfersByUserId(userId);
        return histories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Get transfers by direction (SENT or RECEIVED)
     */
    public List<TransferHistoryResponse> getTransfersByDirection(Long userId, String direction) {
        List<TransferHistory> histories = transferHistoryRepository.findByUserIdAndDirectionOrderByCreatedAtDesc(userId, direction);
        return histories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Get transfers by room
     */
    public List<TransferHistoryResponse> getTransfersByRoom(Long userId, Long roomId) {
        List<TransferHistory> histories = transferHistoryRepository.findByUserIdAndRoomIdOrderByCreatedAtDesc(userId, roomId);
        return histories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Get transfer by ID
     */
    public TransferHistoryResponse getTransferById(Long historyId) {
        TransferHistory history = transferHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Transfer history not found: " + historyId));
        return toResponse(history);
    }

    /**
     * Get transfer statistics for a user
     */
    public TransferStatsResponse getTransferStats(Long userId) {
        Long pendingCount = transferHistoryRepository.countByUserIdAndStatus(userId, STATUS_PENDING);
        Long inProgressCount = transferHistoryRepository.countByUserIdAndStatus(userId, STATUS_IN_PROGRESS);
        Long completedCount = transferHistoryRepository.countByUserIdAndStatus(userId, STATUS_COMPLETED);
        Long abortedCount = transferHistoryRepository.countByUserIdAndStatus(userId, STATUS_ABORTED);
        Long failedCount = transferHistoryRepository.countByUserIdAndStatus(userId, STATUS_FAILED);
        Long sentCount = transferHistoryRepository.countByUserIdAndDirection(userId, DIRECTION_SENT);
        Long receivedCount = transferHistoryRepository.countByUserIdAndDirection(userId, DIRECTION_RECEIVED);

        return TransferStatsResponse.builder()
                .totalTransfers(pendingCount + inProgressCount + completedCount + abortedCount + failedCount)
                .pendingCount(pendingCount)
                .inProgressCount(inProgressCount)
                .completedCount(completedCount)
                .abortedCount(abortedCount)
                .failedCount(failedCount)
                .sentCount(sentCount)
                .receivedCount(receivedCount)
                .activeCount(pendingCount + inProgressCount)
                .build();
    }

    /**
     * Abort a transfer
     */
    @Transactional
    public TransferHistoryResponse abortTransfer(Long userId, Long historyId, String reason) {
        TransferHistory history = transferHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Transfer history not found: " + historyId));

        if (!history.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to abort this transfer");
        }

        if (STATUS_COMPLETED.equals(history.getStatus()) || STATUS_ABORTED.equals(history.getStatus())) {
            throw new RuntimeException("Cannot abort transfer with status: " + history.getStatus());
        }

        history.setStatus(STATUS_ABORTED);
        history.setErrorReason(reason != null ? reason : "Aborted by user");
        history.setCompletedAt(LocalDateTime.now());
        history.setUpdatedAt(LocalDateTime.now());

        return toResponse(transferHistoryRepository.save(history));
    }

    private TransferHistoryListResponse buildListResponse(Page<TransferHistory> page) {
        List<TransferHistoryResponse> transfers = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return TransferHistoryListResponse.builder()
                .transfers(transfers)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    private TransferHistoryResponse toResponse(TransferHistory history) {
        return TransferHistoryResponse.builder()
                .id(history.getId())
                .roomId(history.getRoomId())
                .fileId(history.getFileId())
                .fileName(history.getFileName())
                .fileSize(history.getFileSize())
                .contentType(history.getContentType())
                .userId(history.getUserId())
                .peerId(history.getPeerId())
                .peerName(history.getPeerName())
                .totalPeers(history.getTotalPeers())
                .direction(history.getDirection())
                .status(history.getStatus())
                .errorReason(history.getErrorReason())
                .progressPercentage(history.getProgressPercentage())
                .bytesTransferred(history.getBytesTransferred())
                .startedAt(history.getStartedAt())
                .completedAt(history.getCompletedAt())
                .createdAt(history.getCreatedAt())
                .updatedAt(history.getUpdatedAt())
                .build();
    }
}