package com.example.OpShare.service;

import com.example.OpShare.dto.*;
import com.example.OpShare.entity.Peer;
import com.example.OpShare.entity.Room;
import com.example.OpShare.repository.peerRepository;
import com.example.OpShare.repository.roomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class roomService {

    private final roomRepository roomRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String ROOM_PEERS_KEY_PREFIX = "room:peers:";
    private final peerRepository peerRepository;

    @Transactional
    public RoomResponse createRoom(Long userId) {
        log.error("Creating room for user: {}", userId);

        Room room = Room.builder()
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .active(true)
                .build();

        Room savedRoom = roomRepository.save(room);

        // Add creator as first peer in Redis set
        String redisKey = ROOM_PEERS_KEY_PREFIX + savedRoom.getId();
        redisTemplate.opsForSet().add(redisKey, String.valueOf(userId));

        log.error("Room created with ID: {}", savedRoom.getId());

        return RoomResponse.builder()
                .roomId(savedRoom.getId())
                .createdBy(savedRoom.getCreatedBy())
                .createdAt(savedRoom.getCreatedAt())
                .active(savedRoom.isActive())
                .peers(Set.of(String.valueOf(userId)))
                .message("Room created successfully")
                .build();
    }

    @Transactional
    public RoomResponse joinRoom(Long roomId, Long userId) {

        Optional<Peer> peer = peerRepository.findById(userId);
        if (peer.isEmpty()) {
            throw new RuntimeException("Peer not found with ID: " + userId);
        }

        JoinRoomRequest request = new JoinRoomRequest(userId, peer.get().getName());

        log.error("Peer {} joining room {}", request.getPeerId(), roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + roomId));

        if (!room.isActive()) {
            throw new RuntimeException("Room is no longer active");
        }

        // Add peer to Redis set
        String redisKey = ROOM_PEERS_KEY_PREFIX + roomId;
        redisTemplate.opsForSet().add(redisKey, String.valueOf(request.getPeerId()));

        // Get all peers in the room
        Set<String> peers = redisTemplate.opsForSet().members(redisKey);

        // Broadcast USER_JOINED event
        RoomEvent event = RoomEvent.builder()
                .eventType("USER_JOINED")
                .roomId(roomId)
                .peerId(request.getPeerId())
                .peerName(request.getPeerName())
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId, event);
        log.error("Broadcasted USER_JOINED event for peer {} in room {}", request.getPeerId(), roomId);

        // Update room timestamp
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        return RoomResponse.builder()
                .roomId(room.getId())
                .createdBy(room.getCreatedBy())
                .createdAt(room.getCreatedAt())
                .active(room.isActive())
                .peers(peers)
                .message("Joined room successfully")
                .build();
    }

    @Transactional
    public RoomResponse leaveRoom(Long roomId, Long userId) {
        log.error("Peer {} leaving room {}", userId, roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + roomId));

        // Remove peer from Redis set
        String redisKey = ROOM_PEERS_KEY_PREFIX + roomId;
        redisTemplate.opsForSet().remove(redisKey, String.valueOf(userId));

        // Get remaining peers
        Set<String> remainingPeers = redisTemplate.opsForSet().members(redisKey);

        // Broadcast USER_LEFT event
        RoomEvent event = RoomEvent.builder()
                .eventType("USER_LEFT")
                .roomId(roomId)
                .peerId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId, event);
        log.error("Broadcasted USER_LEFT event for peer {} in room {}", userId, roomId);

        // Check if room is empty and cleanup
        if (remainingPeers == null || remainingPeers.isEmpty()) {
            log.error("Room {} is empty, performing cleanup", roomId);
            cleanupRoom(roomId, room);

            return RoomResponse.builder()
                    .roomId(room.getId())
                    .createdBy(room.getCreatedBy())
                    .createdAt(room.getCreatedAt())
                    .active(false)
                    .peers(Set.of())
                    .message("Left room successfully. Room has been closed as it is now empty.")
                    .build();
        }

        // Update room timestamp
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        return RoomResponse.builder()
                .roomId(room.getId())
                .createdBy(room.getCreatedBy())
                .createdAt(room.getCreatedAt())
                .active(room.isActive())
                .peers(remainingPeers)
                .message("Left room successfully")
                .build();
    }

    private void cleanupRoom(Long roomId, Room room) {
        // Delete Redis key for room peers
        String redisKey = ROOM_PEERS_KEY_PREFIX + roomId;
        redisTemplate.delete(redisKey);
        log.error("Deleted Redis key for room {}", roomId);

        // Mark room as inactive in database
        room.setActive(false);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);
        log.error("Marked room {} as inactive", roomId);

        // Broadcast ROOM_CLOSED event
        RoomEvent event = RoomEvent.builder()
                .eventType("ROOM_CLOSED")
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId, event);
        log.error("Broadcasted ROOM_CLOSED event for room {}", roomId);
    }

    public Set<String> getRoomPeers(Long roomId) {
        String redisKey = ROOM_PEERS_KEY_PREFIX + roomId;
        return redisTemplate.opsForSet().members(redisKey);
    }
}
