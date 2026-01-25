package com.example.OpShare.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transfer_history")
public class TransferHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private BigInteger fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "peer_id")
    private Long peerId;

    @Column(name = "peer_name")
    private String peerName;

    @Column(name = "total_peers")
    private Integer totalPeers;

    @Column(name = "direction")
    private String direction; // SENT, RECEIVED

    @Column(name = "status")
    private String status; // PENDING, IN_PROGRESS, COMPLETED, ABORTED, FAILED

    @Column(name = "error_reason")
    private String errorReason;

    @Column(name = "progress_percentage")
    private Integer progressPercentage;

    @Column(name = "bytes_transferred")
    private BigInteger bytesTransferred;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}