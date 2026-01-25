package com.example.OpShare.controller;

import com.example.OpShare.dto.*;
import com.example.OpShare.service.ChunkedUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@CrossOrigin
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class ChunkedUploadController {

    private final ChunkedUploadService chunkedUploadService;

    @PostMapping("/init")
    public ResponseEntity<InitUploadResponse> initializeUpload(
            @RequestBody InitUploadRequest request,
            Principal principal) {
        log.error("Received upload init request for file: {}", request.getFileName());
        Long userId = Long.parseLong(principal.getName());
        InitUploadResponse response = chunkedUploadService.initializeUpload(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/{uploadId}/chunk/{chunkNumber}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChunkUploadResponse> uploadChunk(
            @PathVariable String uploadId,
            @PathVariable Integer chunkNumber,
            @RequestParam("chunk") MultipartFile chunk) throws IOException {
        log.error("Received chunk {} for upload: {}", chunkNumber, uploadId);
        ChunkUploadResponse response = chunkedUploadService.uploadChunk(uploadId, chunkNumber, chunk);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{uploadId}/complete")
    public ResponseEntity<CompleteUploadResponse> completeUpload(
            @PathVariable String uploadId) throws IOException {
        log.error("Received complete request for upload: {}", uploadId);
        CompleteUploadResponse response = chunkedUploadService.completeUpload(uploadId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uploadId}/progress")
    public ResponseEntity<UploadProgressResponse> getProgress(
            @PathVariable String uploadId) {
        log.error("Received progress request for upload: {}", uploadId);
        UploadProgressResponse response = chunkedUploadService.getUploadProgress(uploadId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{uploadId}")
    public ResponseEntity<Map<String, String>> cancelUpload(
            @PathVariable String uploadId) {
        log.error("Received cancel request for upload: {}", uploadId);
        chunkedUploadService.cancelUpload(uploadId);
        return ResponseEntity.ok(Map.of("message", "Upload cancelled"));
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, Object>> checkDuplicate(
            @RequestParam String fileHash,
            @RequestParam Long roomId) {
        log.error("Checking duplicate for hash: {} in room: {}", fileHash, roomId);
        boolean exists = chunkedUploadService.checkDuplicate(fileHash, roomId);
        return ResponseEntity.ok(Map.of(
                "exists", exists,
                "message", exists ? "File already exists in this room" : "No duplicate found"
        ));
    }
}