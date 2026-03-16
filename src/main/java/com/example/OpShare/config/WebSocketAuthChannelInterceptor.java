package com.example.OpShare.config;

import com.example.OpShare.service.AuthService;
import com.example.OpShare.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Intercepts the STOMP CONNECT frame and authenticates the user via JWT.
 *
 * The HTTP servlet filter (JwtAuthenticationFilter) only runs on the initial
 * HTTP upgrade handshake — not on subsequent STOMP frames. This interceptor
 * fills that gap by reading the token from the STOMP "Authorization" native header.
 *
 * Client must send:
 *   stompClient.connect({ "Authorization": "Bearer <token>" }, ...)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final AuthService authService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("STOMP CONNECT received without Authorization header — closing connection");
            // Returning null rejects the message and closes the connection
            return null;
        }

        try {
            String jwt = authHeader.substring(7);

            if (jwtService.validateToken(jwt) && authService.validateSession(jwt)) {
                Long userId = jwtService.getUserIdFromToken(jwt);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        Collections.emptyList()
                );

                accessor.setUser(auth);
                log.debug("STOMP CONNECT authenticated for user {}", userId);
            } else {
                log.warn("STOMP CONNECT: invalid or expired token");
                return null;
            }
        } catch (Exception e) {
            log.error("STOMP CONNECT auth failed: {}", e.getMessage());
            return null;
        }

        return message;
    }
}