package com.example.OpShare.service;

import com.example.OpShare.dto.PeerResponse;
import com.example.OpShare.dto.UpdatePeerRequest;
import com.example.OpShare.entity.Peer;
import com.example.OpShare.repository.peerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeerService {

    private final peerRepository peerRepository;

    public PeerResponse getPeer(Long peerId) {
        Peer peer = peerRepository.findById(peerId)
                .orElseThrow(() -> new RuntimeException("Peer not found with ID: " + peerId));

        return toPeerResponse(peer, null);
    }

    public PeerResponse getPeerByContactNumber(String contactNumber) {
        Peer peer = peerRepository.findByContactNumber(contactNumber)
                .orElseThrow(() -> new RuntimeException("Peer not found with contact number: " + contactNumber));

        return toPeerResponse(peer, null);
    }

    @Transactional
    public PeerResponse updatePeer(Long peerId, UpdatePeerRequest request) {
        log.error("Updating peer ID: {}", peerId);

        Peer peer = peerRepository.findById(peerId)
                .orElseThrow(() -> new RuntimeException("Peer not found with ID: " + peerId));

        if (request.getName() != null && !request.getName().isEmpty()) {
            peer.setName(request.getName());
        }

        peer.setUpdatedAt(LocalDateTime.now());
        peerRepository.save(peer);

        log.error("Peer updated successfully: {}", peerId);
        return toPeerResponse(peer, "Peer updated successfully");
    }

    @Transactional
    public void updateLastSeen(Long peerId) {
        peerRepository.findById(peerId).ifPresent(peer -> {
            peer.setLastSeen(LocalDateTime.now());
            peerRepository.save(peer);
        });
    }

    @Transactional
    public void deactivatePeer(Long peerId) {
        Peer peer = peerRepository.findById(peerId)
                .orElseThrow(() -> new RuntimeException("Peer not found with ID: " + peerId));

        peer.setActive(false);
        peer.setUpdatedAt(LocalDateTime.now());
        peerRepository.save(peer);

        log.error("Peer deactivated: {}", peerId);
    }

    public boolean existsByContactNumber(String contactNumber) {
        return peerRepository.existsByContactNumber(contactNumber);
    }

    @Transactional
    public void setInactive(Long peerId) {
        peerRepository.findById(peerId).ifPresent(peer -> {
            peer.setActive(false);
            peer.setUpdatedAt(LocalDateTime.now());
            peerRepository.save(peer);
        });
    }

    public List<PeerResponse> getActivePeersByIp(String ip, Long userId) {
        return peerRepository.findByIpAndActiveTrue(ip).stream()
                .map(peer -> toPeerResponse(peer, null))
                .filter(peer -> !Objects.equals(peer.getId(), userId))
                .toList();
    }

    public String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private PeerResponse toPeerResponse(Peer peer, String message) {
        return PeerResponse.builder()
                .id(peer.getId())
                .name(peer.getName())
                .contactNumber(peer.getContactNumber())
                .verified(peer.isVerified())
                .active(peer.isActive())
                .createdAt(peer.getCreatedAt())
                .lastSeen(peer.getLastSeen())
                .message(message)
                .build();
    }
}