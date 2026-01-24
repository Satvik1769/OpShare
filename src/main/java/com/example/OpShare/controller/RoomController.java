package com.example.OpShare.controller;

import com.example.OpShare.dto.*;
import com.example.OpShare.service.roomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/rooms")
@CrossOrigin
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class RoomController {

    private final roomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        log.error("Received request to create room");
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<RoomResponse> joinRoom(
            @PathVariable Long roomId,
            @RequestBody JoinRoomRequest request) {
        log.error("Received request to join room {}", roomId);
        RoomResponse response = roomService.joinRoom(roomId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<RoomResponse> leaveRoom(
            @PathVariable Long roomId,
            @RequestBody LeaveRoomRequest request) {
        log.error("Received request to leave room {}", roomId);
        RoomResponse response = roomService.leaveRoom(roomId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}/peers")
    public ResponseEntity<Set<String>> getRoomPeers(@PathVariable Long roomId) {
        log.error("Received request to get peers for room {}", roomId);
        Set<String> peers = roomService.getRoomPeers(roomId);
        return ResponseEntity.ok(peers);
    }
}
