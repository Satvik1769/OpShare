package com.example.OpShare.service;

import com.example.OpShare.dto.FileEvent;
import com.example.OpShare.dto.RoomEvent;
import com.example.OpShare.dto.UploadEvent;
import com.example.OpShare.entity.FileUploadSession;
import com.example.OpShare.entity.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // Upload Events
    public void notifyUploadStarted(FileUploadSession session) {
        UploadEvent event = UploadEvent.builder()
                .eventType(UploadEvent.UPLOAD_STARTED)
                .uploadId(session.getUploadId())
                .fileName(session.getFileName())
                .roomId(session.getRoomId())
                .userId(session.getUploadedBy())
                .totalBytes(session.getFileSize())
                .totalChunks(session.getTotalChunks())
                .uploadedBytes(BigInteger.ZERO)
                .uploadedChunks(0)
                .progressPercent(0.0)
                .status(session.getStatus())
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(session.getRoomId(), event);
        broadcastToUpload(session.getUploadId(), event);
        log.info("Pushed UPLOAD_STARTED event for upload: {}", session.getUploadId());
    }

    public void notifyUploadProgress(FileUploadSession session, double progressPercent) {
        UploadEvent event = UploadEvent.builder()
                .eventType(UploadEvent.UPLOAD_PROGRESS)
                .uploadId(session.getUploadId())
                .fileName(session.getFileName())
                .roomId(session.getRoomId())
                .userId(session.getUploadedBy())
                .uploadedBytes(session.getUploadedBytes())
                .totalBytes(session.getFileSize())
                .uploadedChunks(session.getUploadedChunks())
                .totalChunks(session.getTotalChunks())
                .progressPercent(progressPercent)
                .status(session.getStatus())
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(session.getRoomId(), event);
        broadcastToUpload(session.getUploadId(), event);
    }

    public void notifyUploadCompleted(FileUploadSession session, Files file) {
        UploadEvent event = UploadEvent.builder()
                .eventType(UploadEvent.UPLOAD_COMPLETED)
                .uploadId(session.getUploadId())
                .fileId(file.getId())
                .fileName(file.getFileName())
                .roomId(file.getRoomId())
                .userId(file.getUploadedBy())
                .uploadedBytes(file.getFileSize())
                .totalBytes(file.getFileSize())
                .uploadedChunks(session.getTotalChunks())
                .totalChunks(session.getTotalChunks())
                .progressPercent(100.0)
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(session.getRoomId(), event);
        broadcastToUpload(session.getUploadId(), event);
        log.info("Pushed UPLOAD_COMPLETED event for file: {}", file.getId());
    }

    public void notifyUploadFailed(FileUploadSession session, String error) {
        UploadEvent event = UploadEvent.builder()
                .eventType(UploadEvent.UPLOAD_FAILED)
                .uploadId(session.getUploadId())
                .fileName(session.getFileName())
                .roomId(session.getRoomId())
                .userId(session.getUploadedBy())
                .uploadedBytes(session.getUploadedBytes())
                .totalBytes(session.getFileSize())
                .uploadedChunks(session.getUploadedChunks())
                .totalChunks(session.getTotalChunks())
                .status("FAILED")
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(session.getRoomId(), event);
        broadcastToUpload(session.getUploadId(), event);
        log.error("Pushed UPLOAD_FAILED event for upload: {}", session.getUploadId());
    }

    public void notifyUploadCancelled(FileUploadSession session) {
        UploadEvent event = UploadEvent.builder()
                .eventType(UploadEvent.UPLOAD_CANCELLED)
                .uploadId(session.getUploadId())
                .fileName(session.getFileName())
                .roomId(session.getRoomId())
                .userId(session.getUploadedBy())
                .status("CANCELLED")
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(session.getRoomId(), event);
        broadcastToUpload(session.getUploadId(), event);
        log.info("Pushed UPLOAD_CANCELLED event for upload: {}", session.getUploadId());
    }

    public void notifyFileDeduplicated(Long roomId, Long userId, Long existingFileId, String fileName) {
        UploadEvent event = UploadEvent.builder()
                .eventType(UploadEvent.FILE_DEDUPLICATED)
                .fileId(existingFileId)
                .fileName(fileName)
                .roomId(roomId)
                .userId(userId)
                .status("DEDUPLICATED")
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(roomId, event);
        log.info("Pushed FILE_DEDUPLICATED event for file: {}", existingFileId);
    }

    // File Events
    public void notifyFileOffered(Files file, Long toUserId) {
        FileEvent event = FileEvent.builder()
                .eventType("FILE_OFFERED")
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .roomId(file.getRoomId())
                .fromUserId(file.getUploadedBy())
                .toUserId(toUserId)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(file.getRoomId(), event);
        sendToUser(toUserId, event);
    }

    public void notifyFileAccepted(Files file, Long byUserId) {
        FileEvent event = FileEvent.builder()
                .eventType("FILE_ACCEPTED")
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .roomId(file.getRoomId())
                .fromUserId(file.getUploadedBy())
                .toUserId(byUserId)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(file.getRoomId(), event);
        sendToUser(file.getUploadedBy(), event);
    }

    public void notifyFileRejected(Files file, Long byUserId) {
        FileEvent event = FileEvent.builder()
                .eventType("FILE_REJECTED")
                .fileId(file.getId())
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .roomId(file.getRoomId())
                .fromUserId(file.getUploadedBy())
                .toUserId(byUserId)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(file.getRoomId(), event);
        sendToUser(file.getUploadedBy(), event);
    }

    // Room Events
    public void notifyUserJoined(Long roomId, Long userId, String userName) {
        RoomEvent event = RoomEvent.builder()
                .eventType("USER_JOINED")
                .roomId(roomId)
                .peerId(userId)
                .peerName(userName)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(roomId, event);
    }

    public void notifyUserLeft(Long roomId, Long userId) {
        RoomEvent event = RoomEvent.builder()
                .eventType("USER_LEFT")
                .roomId(roomId)
                .peerId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(roomId, event);
    }

    public void notifyRoomClosed(Long roomId) {
        RoomEvent event = RoomEvent.builder()
                .eventType("ROOM_CLOSED")
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(roomId, event);
    }

    // Private helper methods
    private void broadcastToRoom(Long roomId, Object event) {
        String destination = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(destination, event);
    }

    private void broadcastToUpload(String uploadId, Object event) {
        String destination = "/topic/upload/" + uploadId;
        messagingTemplate.convertAndSend(destination, event);
    }

    private void sendToUser(Long userId, Object event) {
        String destination = "/queue/notifications";
        messagingTemplate.convertAndSendToUser(userId.toString(), destination, event);
    }
}