package com.example.OpShare.repository;

import com.example.OpShare.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface otpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByContactNumberAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String contactNumber, LocalDateTime now);

    List<Otp> findByContactNumberAndVerifiedFalse(String contactNumber);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}