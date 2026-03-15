package com.example.OpShare.controller;

import com.example.OpShare.dto.SignalMessage;
import com.example.OpShare.dto.SubscribeRequest;
import io.netty.util.Signal;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

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
            log.error("User {} subscribed to room {} (session: {})", principal.getName(), request.getRoomId(), sessionId);
        }
    }

    @MessageMapping("/subscribe/upload")
    public void subscribeToUpload(@Payload SubscribeRequest request, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal != null) {
            String sessionId = headerAccessor.getSessionId();
            log.error("User {} subscribed to upload {} (session: {})", principal.getName(), request.getUploadId(), sessionId);
        }
    }

    @MessageMapping("/ping")
    public void handlePing(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal != null) {
            log.debug("Received ping from user: {}", principal.getName());
        }
    }

    @MessageMapping("/signal/{roomCode}")
    public void handleSignal(SignalMessage message, @DestinationVariable String roomCode, Principal principal) {
        log.error("Received signal for room {}: {}", roomCode, message);
        // forward offer/answer/ice to the other peer in the room
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode, message);

    }
}
