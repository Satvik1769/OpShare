package com.example.OpShare.repository;

import com.example.OpShare.entity.Files;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface fileRepository extends JpaRepository<Files, Long> {

    List<Files> findByRoomId(Long roomId);

    List<Files> findByUploadedBy(Long uploadedBy);

    List<Files> findByRoomIdAndStatus(Long roomId, String status);

    Optional<Files> findByHashAndRoomId(String hash, Long roomId);

    Optional<Files> findByHash(String hash);

    boolean existsByHashAndRoomId(String hash, Long roomId);
}