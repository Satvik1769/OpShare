package com.example.OpShare.repository;

import com.example.OpShare.compositeEntity.FileAccessId;
import com.example.OpShare.entity.FileAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface fileAccessRepository extends JpaRepository<FileAccess, FileAccessId> {

    List<FileAccess> findByFileId(Long fileId);

    List<FileAccess> findByUserId(Long userId);

    Optional<FileAccess> findByFileIdAndUserId(Long fileId, Long userId);

    List<FileAccess> findByUserIdAndStatus(Long userId, String status);
}