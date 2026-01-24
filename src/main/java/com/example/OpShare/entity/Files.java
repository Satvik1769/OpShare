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
@Table(name = "files")
public class Files {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "room_id")
    private Long roomId;
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "file_size")
    private BigInteger fileSize;
    @Column(name = "status")
    private String status;
    @Column(name = "file_path")
    private String filePath;
    @Column(name = "hash")
    private String hash;
    @Column(name = "uploaded_by")
    private Long uploadedBy;
}
