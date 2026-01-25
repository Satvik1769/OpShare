package com.example.OpShare.controller;

import com.example.OpShare.dto.TransferHistoryListResponse;
import com.example.OpShare.dto.TransferHistoryResponse;
import com.example.OpShare.dto.TransferStatsResponse;
import com.example.OpShare.service.TransferHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/history")
@CrossOrigin
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class TransferHistoryController {

    private final TransferHistoryService transferHistoryService;

    /**
     * Get all transfer history for the authenticated user (paginated)
     * GET /history?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<TransferHistoryListResponse> getAllTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        log.info("Getting all transfers for user, page: {}, size: {}", page, size);
        Long userId = Long.parseLong(principal.getName());
        TransferHistoryListResponse response = transferHistoryService.getAllTransfers(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get active transfers (PENDING or IN_PROGRESS)
     * GET /history/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<TransferHistoryResponse>> getActiveTransfers(Principal principal) {
        log.info("Getting active transfers for user");
        Long userId = Long.parseLong(principal.getName());
        List<TransferHistoryResponse> transfers = transferHistoryService.getActiveTransfers(userId);
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfers by status
     * GET /history/status/{status}?page=0&size=20
     * Status: PENDING, IN_PROGRESS, COMPLETED, ABORTED, FAILED
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<TransferHistoryListResponse> getTransfersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        log.info("Getting transfers by status: {}", status);
        Long userId = Long.parseLong(principal.getName());
        TransferHistoryListResponse response = transferHistoryService.getTransfersByStatus(userId, status.toUpperCase(), page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transfers by direction
     * GET /history/direction/{direction}
     * Direction: SENT, RECEIVED
     */
    @GetMapping("/direction/{direction}")
    public ResponseEntity<List<TransferHistoryResponse>> getTransfersByDirection(
            @PathVariable String direction,
            Principal principal) {
        log.info("Getting transfers by direction: {}", direction);
        Long userId = Long.parseLong(principal.getName());
        List<TransferHistoryResponse> transfers = transferHistoryService.getTransfersByDirection(userId, direction.toUpperCase());
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get sent transfers
     * GET /history/sent
     */
    @GetMapping("/sent")
    public ResponseEntity<List<TransferHistoryResponse>> getSentTransfers(Principal principal) {
        log.info("Getting sent transfers for user");
        Long userId = Long.parseLong(principal.getName());
        List<TransferHistoryResponse> transfers = transferHistoryService.getTransfersByDirection(userId, TransferHistoryService.DIRECTION_SENT);
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get received transfers
     * GET /history/received
     */
    @GetMapping("/received")
    public ResponseEntity<List<TransferHistoryResponse>> getReceivedTransfers(Principal principal) {
        log.info("Getting received transfers for user");
        Long userId = Long.parseLong(principal.getName());
        List<TransferHistoryResponse> transfers = transferHistoryService.getTransfersByDirection(userId, TransferHistoryService.DIRECTION_RECEIVED);
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfers by room
     * GET /history/room/{roomId}
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<TransferHistoryResponse>> getTransfersByRoom(
            @PathVariable Long roomId,
            Principal principal) {
        log.info("Getting transfers for room: {}", roomId);
        Long userId = Long.parseLong(principal.getName());
        List<TransferHistoryResponse> transfers = transferHistoryService.getTransfersByRoom(userId, roomId);
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfer details by ID
     * GET /history/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransferHistoryResponse> getTransferById(@PathVariable Long id) {
        log.info("Getting transfer details for id: {}", id);
        TransferHistoryResponse response = transferHistoryService.getTransferById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transfer statistics
     * GET /history/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<TransferStatsResponse> getTransferStats(Principal principal) {
        log.info("Getting transfer statistics for user");
        Long userId = Long.parseLong(principal.getName());
        TransferStatsResponse stats = transferHistoryService.getTransferStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Abort a transfer
     * POST /history/{id}/abort
     */
    @PostMapping("/{id}/abort")
    public ResponseEntity<TransferHistoryResponse> abortTransfer(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Principal principal) {
        log.info("Aborting transfer: {}", id);
        Long userId = Long.parseLong(principal.getName());
        TransferHistoryResponse response = transferHistoryService.abortTransfer(userId, id, reason);
        return ResponseEntity.ok(response);
    }
}