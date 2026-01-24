package com.example.OpShare.controller;

import com.example.OpShare.dto.*;
import com.example.OpShare.entity.FileAccess;
import com.example.OpShare.entity.Files;
import com.example.OpShare.service.fileService;
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
import java.util.List;

@RestController
@RequestMapping("/files")
@CrossOrigin
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class FileController {

    private final fileService fileService;

    @PostMapping(value = "/upload/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {
        log.error("Received file upload request for room {}", roomId);
        Long userId = Long.parseLong(principal.getName());
        FileUploadResponse response = fileService.uploadFile(roomId, userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/offer")
    public ResponseEntity<FileOfferResponse> offerFile(
            @RequestBody OfferFileRequest request,
            Principal principal) {
        log.error("Received file offer request for file {}", request.getFileId());
        Long userId = Long.parseLong(principal.getName());
        FileOfferResponse response = fileService.offerFile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{fileId}/accept")
    public ResponseEntity<FileOfferResponse> acceptFile(
            @PathVariable Long fileId,
            Principal principal) {
        log.error("Received file accept request for file {}", fileId);
        Long userId = Long.parseLong(principal.getName());
        FileOfferResponse response = fileService.acceptFile(userId, fileId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{fileId}/reject")
    public ResponseEntity<FileOfferResponse> rejectFile(
            @PathVariable Long fileId,
            Principal principal) {
        log.error("Received file reject request for file {}", fileId);
        Long userId = Long.parseLong(principal.getName());
        FileOfferResponse response = fileService.rejectFile(userId, fileId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<FileDownloadResponse> getDownloadUrl(
            @PathVariable Long fileId,
            Principal principal) {
        log.error("Received download URL request for file {}", fileId);
        Long userId = Long.parseLong(principal.getName());
        FileDownloadResponse response = fileService.getDownloadUrl(userId, fileId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<Files>> getFilesByRoom(@PathVariable Long roomId) {
        log.error("Received request to get files for room {}", roomId);
        List<Files> files = fileService.getFilesByRoom(roomId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/pending-offers")
    public ResponseEntity<List<FileAccess>> getPendingOffers(Principal principal) {
        log.error("Received request to get pending offers");
        Long userId = Long.parseLong(principal.getName());
        List<FileAccess> pendingOffers = fileService.getPendingOffersForUser(userId);
        return ResponseEntity.ok(pendingOffers);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long fileId,
            Principal principal) {
        log.error("Received delete request for file {}", fileId);
        fileService.deleteFileFromS3(fileId);
        return ResponseEntity.noContent().build();
    }
}