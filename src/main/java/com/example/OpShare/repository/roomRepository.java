package com.example.OpShare.repository;

import com.example.OpShare.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface roomRepository extends JpaRepository<Room, Long> {
}
