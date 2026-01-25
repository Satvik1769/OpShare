package com.example.OpShare.controller;

import com.example.OpShare.dto.PeerResponse;
import com.example.OpShare.dto.UpdatePeerRequest;
import com.example.OpShare.service.PeerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/peers")
@CrossOrigin
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class PeerController {

    private final PeerService peerService;

    @GetMapping("/me")
    public ResponseEntity<PeerResponse> getCurrentPeer(Principal principal) {
        log.error("Received get current peer request");
        Long userId = Long.parseLong(principal.getName());
        PeerResponse response = peerService.getPeer(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{peerId}")
    public ResponseEntity<PeerResponse> getPeer(@PathVariable Long peerId) {
        log.error("Received get peer request for ID: {}", peerId);
        PeerResponse response = peerService.getPeer(peerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<PeerResponse> updateCurrentPeer(
            @RequestBody UpdatePeerRequest request,
            Principal principal) {
        log.error("Received update peer request");
        Long userId = Long.parseLong(principal.getName());
        PeerResponse response = peerService.updatePeer(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/heartbeat")
    public ResponseEntity<Map<String, String>> heartbeat(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        peerService.updateLastSeen(userId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deactivateAccount(Principal principal) {
        log.error("Received deactivate account request");
        Long userId = Long.parseLong(principal.getName());
        peerService.deactivatePeer(userId);
        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully"));
    }

    @GetMapping("/check/{contactNumber}")
    public ResponseEntity<Map<String, Boolean>> checkContactNumber(@PathVariable String contactNumber) {
        boolean exists = peerService.existsByContactNumber(contactNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}