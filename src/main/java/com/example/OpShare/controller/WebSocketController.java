package com.example.OpShare.controller;

import com.example.OpShare.dto.SubscribeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

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
}
