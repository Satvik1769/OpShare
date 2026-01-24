package com.example.OpShare.repository;

import com.example.OpShare.entity.Peer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface peerRepository extends JpaRepository<Peer, Long> {
}
