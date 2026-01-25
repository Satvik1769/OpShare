package com.example.OpShare.repository;

import com.example.OpShare.entity.FileUploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface fileUploadSessionRepository extends JpaRepository<FileUploadSession, Long> {

    Optional<FileUploadSession> findByUploadId(String uploadId);

    Optional<FileUploadSession> findByFileHashAndRoomId(String fileHash, Long roomId);

    List<FileUploadSession> findByUploadedByAndStatus(Long uploadedBy, String status);

    List<FileUploadSession> findByExpiresAtBeforeAndStatusNot(LocalDateTime dateTime, String status);

    void deleteByUploadId(String uploadId);
}