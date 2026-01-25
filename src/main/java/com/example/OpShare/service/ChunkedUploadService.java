package com.example.OpShare.service;

import com.example.OpShare.dto.*;
import com.example.OpShare.entity.FileUploadSession;
import com.example.OpShare.entity.Files;
import com.example.OpShare.entity.Room;
import com.example.OpShare.repository.fileRepository;
import com.example.OpShare.repository.fileUploadSessionRepository;
import com.example.OpShare.repository.roomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkedUploadService {

    private final S3Client s3Client;
    private final fileRepository fileRepository;
    private final fileUploadSessionRepository uploadSessionRepository;
    private final roomRepository roomRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final WebSocketNotificationService notificationService;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final String STATUS_INITIATED = "INITIATED";
    private static final String STATUS_UPLOADING = "UPLOADING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String CHUNK_KEY_PREFIX = "upload:chunk:";
    private static final int DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int SESSION_EXPIRY_HOURS = 24;

    @Transactional
    public InitUploadResponse initializeUpload(Long userId, InitUploadRequest request) {
        log.error("Initializing upload for file: {} in room: {}", request.getFileName(), request.getRoomId());

        // Validate room exists and is active
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));
        if (!room.isActive()) {
            throw new RuntimeException("Room is not active");
        }

        // Check for deduplication using file hash
        if (request.getFileHash() != null && !request.getFileHash().isEmpty()) {
            Optional<Files> existingFile = fileRepository.findByHashAndRoomId(request.getFileHash(), request.getRoomId());
            if (existingFile.isPresent()) {
                log.error("File with same hash already exists in room. Deduplication applied.");
                // Push notification: file deduplicated
                notificationService.notifyFileDeduplicated(
                        request.getRoomId(), userId, existingFile.get().getId(), existingFile.get().getFileName());
                return InitUploadResponse.builder()
                        .deduplicated(true)
                        .existingFileId(existingFile.get().getId())
                        .fileName(existingFile.get().getFileName())
                        .fileSize(existingFile.get().getFileSize())
                        .message("File already exists in this room (deduplicated)")
                        .build();
            }
        }

        // Calculate chunks
        int chunkSize = request.getChunkSize() != null ? request.getChunkSize() : DEFAULT_CHUNK_SIZE;
        int totalChunks = (int) Math.ceil(request.getFileSize().doubleValue() / chunkSize);

        // Generate unique upload ID
        String uploadId = UUID.randomUUID().toString();

        // Create S3 keys
        String primaryKey = String.format("rooms/%d/primary/%s/%s", request.getRoomId(), uploadId, request.getFileName());
        String backupKey = String.format("rooms/%d/backup/%s/%s", request.getRoomId(), uploadId, request.getFileName());

        // Create upload session
        FileUploadSession session = FileUploadSession.builder()
                .uploadId(uploadId)
                .roomId(request.getRoomId())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .contentType(request.getContentType())
                .fileHash(request.getFileHash())
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .uploadedChunks(0)
                .uploadedBytes(BigInteger.ZERO)
                .status(STATUS_INITIATED)
                .s3KeyPrimary(primaryKey)
                .s3KeyBackup(backupKey)
                .uploadedBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(SESSION_EXPIRY_HOURS))
                .build();

        uploadSessionRepository.save(session);

        // Push notification: upload started
        notificationService.notifyUploadStarted(session);

        log.error("Upload session created with ID: {}, total chunks: {}", uploadId, totalChunks);

        return InitUploadResponse.builder()
                .uploadId(uploadId)
                .roomId(request.getRoomId())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .deduplicated(false)
                .expiresAt(session.getExpiresAt())
                .message("Upload initialized successfully")
                .build();
    }

    @Transactional
    public ChunkUploadResponse uploadChunk(String uploadId, Integer chunkNumber, MultipartFile chunk) throws IOException {
        log.error("Uploading chunk {} for upload ID: {}", chunkNumber, uploadId);

        FileUploadSession session = uploadSessionRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload session not found"));

        // Validate session
        if (STATUS_COMPLETED.equals(session.getStatus())) {
            throw new RuntimeException("Upload already completed");
        }
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Upload session expired");
        }
        if (chunkNumber < 0 || chunkNumber >= session.getTotalChunks()) {
            throw new RuntimeException("Invalid chunk number");
        }

        // Store chunk in Redis temporarily
        String chunkKey = CHUNK_KEY_PREFIX + uploadId + ":" + chunkNumber;
        byte[] chunkData = chunk.getBytes();

        // Store chunk data in Redis with expiration
        redisTemplate.opsForValue().set(chunkKey, Base64.getEncoder().encodeToString(chunkData));
        redisTemplate.expire(chunkKey, SESSION_EXPIRY_HOURS, TimeUnit.HOURS);

        // Update session progress
        session.setUploadedChunks(session.getUploadedChunks() + 1);
        session.setUploadedBytes(session.getUploadedBytes().add(BigInteger.valueOf(chunkData.length)));
        session.setStatus(STATUS_UPLOADING);
        session.setUpdatedAt(LocalDateTime.now());
        uploadSessionRepository.save(session);

        // Calculate progress
        double progressPercent = (session.getUploadedBytes().doubleValue() / session.getFileSize().doubleValue()) * 100;

        // Push notification: upload progress
        notificationService.notifyUploadProgress(session, progressPercent);

        log.error("Chunk {} uploaded. Progress: {}%", chunkNumber, String.format("%.2f", progressPercent));

        return ChunkUploadResponse.builder()
                .uploadId(uploadId)
                .chunkNumber(chunkNumber)
                .totalChunks(session.getTotalChunks())
                .uploadedChunks(session.getUploadedChunks())
                .uploadedBytes(session.getUploadedBytes())
                .totalBytes(session.getFileSize())
                .progressPercent(progressPercent)
                .status(session.getStatus())
                .message("Chunk uploaded successfully")
                .build();
    }

    @Transactional
    public CompleteUploadResponse completeUpload(String uploadId) throws IOException {
        log.error("Completing upload for ID: {}", uploadId);

        FileUploadSession session = uploadSessionRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload session not found"));

        if (STATUS_COMPLETED.equals(session.getStatus())) {
            throw new RuntimeException("Upload already completed");
        }

        // Reassemble chunks from Redis
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < session.getTotalChunks(); i++) {
            String chunkKey = CHUNK_KEY_PREFIX + uploadId + ":" + i;
            String chunkData = redisTemplate.opsForValue().get(chunkKey);
            if (chunkData == null) {
                throw new RuntimeException("Missing chunk: " + i);
            }
            outputStream.write(Base64.getDecoder().decode(chunkData));
        }

        byte[] fileBytes = outputStream.toByteArray();

        // Verify hash if provided
        String calculatedHash = generateHash(fileBytes);
        if (session.getFileHash() != null && !session.getFileHash().isEmpty()) {
            if (!session.getFileHash().equals(calculatedHash)) {
                session.setStatus(STATUS_FAILED);
                uploadSessionRepository.save(session);
                throw new RuntimeException("File hash mismatch. Upload corrupted.");
            }
        }

        // Upload to S3 primary
        uploadToS3(session.getS3KeyPrimary(), fileBytes, session.getContentType());
        log.error("Uploaded to primary location: {}", session.getS3KeyPrimary());

        // Upload to S3 backup
        uploadToS3(session.getS3KeyBackup(), fileBytes, session.getContentType());
        log.error("Uploaded to backup location: {}", session.getS3KeyBackup());

        // Create file record
        Files file = Files.builder()
                .roomId(session.getRoomId())
                .fileName(session.getFileName())
                .fileSize(session.getFileSize())
                .contentType(session.getContentType())
                .status(STATUS_COMPLETED)
                .s3KeyPrimary(session.getS3KeyPrimary())
                .s3KeyBackup(session.getS3KeyBackup())
                .filePath(session.getS3KeyPrimary())
                .backupFilePath(session.getS3KeyBackup())
                .hash(calculatedHash)
                .uploadedBy(session.getUploadedBy())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        file = fileRepository.save(file);

        // Update session status
        session.setStatus(STATUS_COMPLETED);
        session.setUpdatedAt(LocalDateTime.now());
        uploadSessionRepository.save(session);

        // Cleanup chunks from Redis
        cleanupChunks(uploadId, session.getTotalChunks());

        // Push notification: upload completed
        notificationService.notifyUploadCompleted(session, file);

        log.error("Upload completed. File ID: {}", file.getId());

        return CompleteUploadResponse.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .contentType(file.getContentType())
                .hash(file.getHash())
                .roomId(file.getRoomId())
                .uploadedBy(file.getUploadedBy())
                .createdAt(file.getCreatedAt())
                .status(file.getStatus())
                .message("Upload completed successfully")
                .build();
    }

    public UploadProgressResponse getUploadProgress(String uploadId) {
        FileUploadSession session = uploadSessionRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload session not found"));

        BigInteger remainingBytes = session.getFileSize().subtract(session.getUploadedBytes());
        int remainingChunks = session.getTotalChunks() - session.getUploadedChunks();
        double progressPercent = (session.getUploadedBytes().doubleValue() / session.getFileSize().doubleValue()) * 100;

        return UploadProgressResponse.builder()
                .uploadId(uploadId)
                .fileName(session.getFileName())
                .fileSize(session.getFileSize())
                .uploadedBytes(session.getUploadedBytes())
                .remainingBytes(remainingBytes)
                .totalChunks(session.getTotalChunks())
                .uploadedChunks(session.getUploadedChunks())
                .remainingChunks(remainingChunks)
                .progressPercent(progressPercent)
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .build();
    }

    @Transactional
    public void cancelUpload(String uploadId) {
        log.error("Cancelling upload: {}", uploadId);

        FileUploadSession session = uploadSessionRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload session not found"));

        // Push notification: upload cancelled
        notificationService.notifyUploadCancelled(session);

        // Cleanup chunks from Redis
        cleanupChunks(uploadId, session.getTotalChunks());

        // Delete session
        uploadSessionRepository.delete(session);

        log.error("Upload cancelled: {}", uploadId);
    }

    public boolean checkDuplicate(String fileHash, Long roomId) {
        return fileRepository.existsByHashAndRoomId(fileHash, roomId);
    }

    private void uploadToS3(String key, byte[] content, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
    }

    private String generateHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate hash", e);
        }
    }

    private void cleanupChunks(String uploadId, int totalChunks) {
        for (int i = 0; i < totalChunks; i++) {
            String chunkKey = CHUNK_KEY_PREFIX + uploadId + ":" + i;
            redisTemplate.delete(chunkKey);
        }
    }
}