package com.example.OpShare.repository;

import com.example.OpShare.entity.TransferHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferHistoryRepository extends JpaRepository<TransferHistory, Long> {

    // Find all transfers for a user (both sent and received)
    List<TransferHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find with pagination
    Page<TransferHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Find by user and direction
    List<TransferHistory> findByUserIdAndDirectionOrderByCreatedAtDesc(Long userId, String direction);

    // Find by user and status
    List<TransferHistory> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    // Find by user and status with pagination
    Page<TransferHistory> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status, Pageable pageable);

    // Find active transfers (PENDING or IN_PROGRESS)
    @Query("SELECT th FROM TransferHistory th WHERE th.userId = :userId AND th.status IN ('PENDING', 'IN_PROGRESS') ORDER BY th.createdAt DESC")
    List<TransferHistory> findActiveTransfersByUserId(@Param("userId") Long userId);

    // Find by room
    List<TransferHistory> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    // Find by file
    List<TransferHistory> findByFileIdOrderByCreatedAtDesc(Long fileId);

    // Find by user and room
    List<TransferHistory> findByUserIdAndRoomIdOrderByCreatedAtDesc(Long userId, Long roomId);

    // Find specific transfer
    Optional<TransferHistory> findByFileIdAndUserIdAndPeerId(Long fileId, Long userId, Long peerId);

    // Find by file and user
    Optional<TransferHistory> findByFileIdAndUserId(Long fileId, Long userId);

    // Count by status for a user
    Long countByUserIdAndStatus(Long userId, String status);

    // Count by direction for a user
    Long countByUserIdAndDirection(Long userId, String direction);

    // Get all statuses for aggregation
    @Query("SELECT th.status, COUNT(th) FROM TransferHistory th WHERE th.userId = :userId GROUP BY th.status")
    List<Object[]> countByStatusForUser(@Param("userId") Long userId);
}