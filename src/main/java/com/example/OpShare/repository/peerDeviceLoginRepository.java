package com.example.OpShare.repository;

import com.example.OpShare.entity.PeerDeviceLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface peerDeviceLoginRepository extends JpaRepository<PeerDeviceLogin, Long> {

    Optional<PeerDeviceLogin> findByAuthKey(String authKey);

    Optional<PeerDeviceLogin> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<PeerDeviceLogin> findByUserId(Long userId);

    List<PeerDeviceLogin> findByUserIdAndStatus(Long userId, String status);

    void deleteByAuthKey(String authKey);
}