package com.example.OpShare.repository;

import com.example.OpShare.entity.Peer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface peerRepository extends JpaRepository<Peer, Long> {

    Optional<Peer> findByContactNumber(String contactNumber);

    boolean existsByContactNumber(String contactNumber);
}
