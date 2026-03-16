package com.example.OpShare.controller;

import com.example.OpShare.dto.SignalMessage;
import com.example.OpShare.dto.SubscribeRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    @NonNull
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/subscribe/room")
    public void subscribeToRoom(@Payload SubscribeRequest request, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal != null) {
            String sessionId = headerAccessor.getSessionId();
            log.debug("User {} subscribed to room {} (session: {})", principal.getName(), request.getRoomId(), sessionId);
        }
    }

    @MessageMapping("/subscribe/upload")
    public void subscribeToUpload(@Payload SubscribeRequest request, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal != null) {
            String sessionId = headerAccessor.getSessionId();
            log.debug("User {} subscribed to upload {} (session: {})", principal.getName(), request.getUploadId(), sessionId);
        }
    }

    @MessageMapping("/ping")
    public void handlePing(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal != null) {
            log.debug("Received ping from user: {}", principal.getName());
        }
    }

    /**
     * WebRTC signaling relay — routes offer/answer/ice-candidate to a specific peer.
     *
     * Client sends to:   /app/signal/{roomCode}
     * Message must include a "to" field (target peer's userId).
     * Server delivers to: /user/{to}/queue/signal
     *
     * Types: "offer" | "answer" | "ice-candidate" | "ready"
     */
    @MessageMapping("/signal/{roomCode}")
    public void handleSignal(@Payload SignalMessage message,
                             @DestinationVariable String roomCode,
                             Principal principal) {
        if (principal == null) {
            log.warn("Unauthenticated signal attempt for room {}", roomCode);
            return;
        }

        if (message.getTo() == null || message.getTo().isBlank()) {
            log.warn("Signal from {} missing 'to' field, dropping", principal.getName());
            return;
        }

        // Always set from server-side — never trust the client-supplied value
        message.setFrom(principal.getName());

        log.debug("Relaying {} signal in room {} from {} to {}",
                message.getType(), roomCode, message.getFrom(), message.getTo());

        // Deliver only to the intended recipient via their user-specific queue
        messagingTemplate.convertAndSendToUser(
                message.getTo(),
                "/queue/signal",
                message
        );
    }
}