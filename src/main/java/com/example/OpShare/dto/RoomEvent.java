package com.example.OpShare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomEvent {
    private String eventType;
    private Long roomId;
    private Long peerId;
    private String peerName;
    private LocalDateTime timestamp;
}