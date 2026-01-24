package com.example.OpShare.service;

import com.example.OpShare.dto.*;
import com.example.OpShare.entity.FileAccess;
import com.example.OpShare.entity.Files;
import com.example.OpShare.entity.Room;
import com.example.OpShare.repository.fileAccessRepository;
import com.example.OpShare.repository.fileRepository;
import com.example.OpShare.repository.roomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class fileService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final fileRepository fileRepository;
    private final fileAccessRepository fileAccessRepository;
    private final roomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiration-minutes}")
    private int presignedUrlExpirationMinutes;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String ROOM_PEERS_KEY_PREFIX = "room:peers:";

    private static final String STATUS_UPLOADED = "UPLOADED";
    private static final String STATUS_OFFERED = "OFFERED";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_PENDING = "PENDING";

    @Transactional
    public FileUploadResponse uploadFile(Long roomId, Long userId, MultipartFile file) throws IOException {
        log.error("Uploading file {} to room {} by user {}", file.getOriginalFilename(), roomId, userId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + roomId));

        if (!room.isActive()) {
            throw new RuntimeException("Room is no longer active");
        }

        String fileId = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        byte[] fileBytes = file.getBytes();

        // Generate hash for file integrity
        String fileHash = generateHash(fileBytes);

        // Create S3 keys for primary and backup storage
        String primaryKey = String.format("rooms/%d/primary/%s/%s", roomId, fileId, originalFilename);
        String backupKey = String.format("rooms/%d/backup/%s/%s", roomId, fileId, originalFilename);

        // Upload to primary location
        uploadToS3(primaryKey, fileBytes, contentType);
        log.error("Uploaded file to primary location: {}", primaryKey);

        // Upload to backup location for durability
        uploadToS3(backupKey, fileBytes, contentType);
        log.error("Uploaded file to backup location: {}", backupKey);

        // Save file metadata to database
        Files savedFile = Files.builder()
                .roomId(roomId)
                .fileName(originalFilename)
                .fileSize(BigInteger.valueOf(file.getSize()))
                .contentType(contentType)
                .status(STATUS_UPLOADED)
                .s3KeyPrimary(primaryKey)
                .s3KeyBackup(backupKey)
                .filePath(primaryKey)
                .backupFilePath(backupKey)
                .hash(fileHash)
                .uploadedBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        savedFile = fileRepository.save(savedFile);

        // Broadcast FILE_UPLOADED event
        FileEvent event = FileEvent.builder()
                .eventType("FILE_UPLOADED")
                .fileId(savedFile.getId())
                .fileName(originalFilename)
                .fileSize(BigInteger.valueOf(file.getSize()))
                .roomId(roomId)
                .fromUserId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId, event);

        return FileUploadResponse.builder()
                .fileId(savedFile.getId())
                .fileName(originalFilename)
                .fileSize(BigInteger.valueOf(file.getSize()))
                .contentType(contentType)
                .status(STATUS_UPLOADED)
                .roomId(roomId)
                .uploadedBy(userId)
                .createdAt(savedFile.getCreatedAt())
                .message("File uploaded successfully")
                .build();
    }

    @Transactional
    public FileOfferResponse offerFile(Long userId, OfferFileRequest request) {
        log.error("User {} offering file {} to room {}", userId, request.getFileId(), request.getRoomId());

        Files file = fileRepository.findById(request.getFileId())
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + request.getFileId()));

        if (!file.getUploadedBy().equals(userId)) {
            throw new RuntimeException("Only the file owner can offer this file");
        }

        // Update file with offer information
        file.setStatus(STATUS_OFFERED);
        file.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(file);

        String redisKey = ROOM_PEERS_KEY_PREFIX + request.getRoomId();
        Set<String> remainingPeers = redisTemplate.opsForSet().members(redisKey);
        // Create file access record

        List<FileAccess> fileAccessList = new ArrayList<>();

        for (String peerId : remainingPeers) {

            FileAccess fileAccess = FileAccess.builder()
                    .fileId(file.getId())
                    .userId(Long.parseLong(peerId))
                    .status(STATUS_PENDING)
                    .offeredAt(LocalDateTime.now())
                    .build();
            fileAccessList.add(fileAccess);

            // Broadcast FILE_OFFERED event
            FileEvent event = FileEvent.builder()
                    .eventType("FILE_OFFERED")
                    .fileId(file.getId())
                    .fileName(file.getFileName())
                    .fileSize(file.getFileSize())
                    .roomId(file.getRoomId())
                    .fromUserId(userId)
                    .toUserId(Long.parseLong(peerId))
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/room/" + file.getRoomId(), event);
        }


        fileAccessRepository.saveAll(fileAccessList);



        return FileOfferResponse.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .contentType(file.getContentType())
                .offeredBy(userId)
                .status(STATUS_OFFERED)
                .offeredAt(LocalDateTime.now())
                .message("File offered successfully")
                .build();
    }

    @Transactional
    public FileOfferResponse acceptFile(Long userId, Long fileId) {
        log.error("User {} accepting file {}", userId, fileId);

        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));

        FileAccess fileAccess = fileAccessRepository.findByFileIdAndUserId(fileId, userId)
                .orElseThrow(() -> new RuntimeException("File offer not found for user"));

        if (!STATUS_PENDING.equals(fileAccess.getStatus())) {
            throw new RuntimeException("File offer has already been " + fileAccess.getStatus().toLowerCase());
        }

        // Update file access status
        fileAccess.setStatus(STATUS_ACCEPTED);
        fileAccess.setAcceptedAt(LocalDateTime.now());
        fileAccessRepository.save(fileAccess);

        // Update file status
        file.setStatus(STATUS_ACCEPTED);
        file.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(file);

        // Broadcast FILE_ACCEPTED event
        FileEvent event = FileEvent.builder()
                .eventType("FILE_ACCEPTED")
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .roomId(file.getRoomId())
                .fromUserId(file.getUploadedBy())
                .toUserId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + file.getRoomId(), event);

        return FileOfferResponse.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .contentType(file.getContentType())
                .offeredBy(file.getUploadedBy())
                .status(STATUS_ACCEPTED)
                .offeredAt(fileAccess.getOfferedAt())
                .message("File accepted successfully")
                .build();
    }

    @Transactional
    public FileOfferResponse rejectFile(Long userId, Long fileId) {
        log.error("User {} rejecting file {}", userId, fileId);

        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));

        FileAccess fileAccess = fileAccessRepository.findByFileIdAndUserId(fileId, userId)
                .orElseThrow(() -> new RuntimeException("File offer not found for user"));

        if (!STATUS_PENDING.equals(fileAccess.getStatus())) {
            throw new RuntimeException("File offer has already been " + fileAccess.getStatus().toLowerCase());
        }

        // Update file access status
        fileAccess.setStatus(STATUS_REJECTED);
        fileAccessRepository.save(fileAccess);

        // Update file status back to uploaded (can be offered to others)
        file.setStatus(STATUS_UPLOADED);
        file.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(file);

        // Broadcast FILE_REJECTED event
        FileEvent event = FileEvent.builder()
                .eventType("FILE_REJECTED")
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .roomId(file.getRoomId())
                .fromUserId(file.getUploadedBy())
                .toUserId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + file.getRoomId(), event);

        return FileOfferResponse.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .contentType(file.getContentType())
                .offeredBy(file.getUploadedBy())
                .status(STATUS_REJECTED)
                .offeredAt(fileAccess.getOfferedAt())
                .message("File rejected")
                .build();
    }

    public FileDownloadResponse getDownloadUrl(Long userId, Long fileId) {
        log.error("User {} requesting download URL for file {}", userId, fileId);

        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));

        // Check if user has access (owner or accepted the file)
        boolean isOwner = file.getUploadedBy().equals(userId);
        boolean hasAccepted = fileAccessRepository.findByFileIdAndUserId(fileId, userId)
                .map(fa -> STATUS_ACCEPTED.equals(fa.getStatus()))
                .orElse(false);

        if (!isOwner && !hasAccepted) {
            throw new RuntimeException("User does not have access to this file");
        }

        // Generate presigned URL
        String presignedUrl = generatePresignedUrl(file.getS3KeyPrimary());
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(presignedUrlExpirationMinutes);

        return FileDownloadResponse.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .presignedUrl(presignedUrl)
                .expiresAt(expiresAt)
                .message("Download URL generated successfully")
                .build();
    }

    public List<Files> getFilesByRoom(Long roomId) {
        return fileRepository.findByRoomId(roomId);
    }

    public List<FileAccess> getPendingOffersForUser(Long userId) {
        return fileAccessRepository.findByUserIdAndStatus(userId, STATUS_PENDING);
    }

    private void uploadToS3(String key, byte[] content, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
    }

    private String generatePresignedUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
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

    @Transactional
    public void deleteFileFromS3(Long fileId) {
        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));

        // Delete from primary location
        deleteFromS3(file.getS3KeyPrimary());
        log.error("Deleted file from primary location: {}", file.getS3KeyPrimary());

        // Delete from backup location
        deleteFromS3(file.getS3KeyBackup());
        log.error("Deleted file from backup location: {}", file.getS3KeyBackup());

        // Delete file record
        fileRepository.delete(file);
    }

    private void deleteFromS3(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }
}