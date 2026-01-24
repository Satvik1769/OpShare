package com.example.OpShare.service;

import com.example.OpShare.dto.PeerResponse;
import com.example.OpShare.dto.UpdatePeerRequest;
import com.example.OpShare.entity.Peer;
import com.example.OpShare.repository.peerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        log.info("Updating peer ID: {}", peerId);

        Peer peer = peerRepository.findById(peerId)
                .orElseThrow(() -> new RuntimeException("Peer not found with ID: " + peerId));

        if (request.getName() != null && !request.getName().isEmpty()) {
            peer.setName(request.getName());
        }

        peer.setUpdatedAt(LocalDateTime.now());
        peerRepository.save(peer);

        log.info("Peer updated successfully: {}", peerId);
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

        log.info("Peer deactivated: {}", peerId);
    }

    public boolean existsByContactNumber(String contactNumber) {
        return peerRepository.existsByContactNumber(contactNumber);
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