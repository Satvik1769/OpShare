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
@Table(name = "file_upload_session")
public class FileUploadSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "upload_id", unique = true, nullable = false)
    private String uploadId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private BigInteger fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_hash")
    private String fileHash;

    @Column(name = "chunk_size")
    private Integer chunkSize;

    @Column(name = "total_chunks")
    private Integer totalChunks;

    @Column(name = "uploaded_chunks")
    private Integer uploadedChunks;

    @Column(name = "uploaded_bytes")
    private BigInteger uploadedBytes;

    @Column(name = "status")
    private String status;

    @Column(name = "s3_key_primary")
    private String s3KeyPrimary;

    @Column(name = "s3_key_backup")
    private String s3KeyBackup;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}