package com.example.OpShare.entity;

import com.example.OpShare.compositeEntity.FileAccessId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "file_access")
@IdClass(FileAccessId.class)
public class FileAccess {

    @Id
    @Column(name = "file_id")
    private Long fileId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
}
