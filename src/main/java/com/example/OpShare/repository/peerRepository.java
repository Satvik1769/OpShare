package com.example.OpShare.repository;

import com.example.OpShare.entity.Peer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface peerRepository extends JpaRepository<Peer, Long> {

    Optional<Peer> findByContactNumber(String contactNumber);

    boolean existsByContactNumber(String contactNumber);

    List<Peer> findByIpAndActiveTrue(String ip);

    List<Peer> findByActiveTrueAndLastSeenBefore(LocalDateTime threshold);
}
