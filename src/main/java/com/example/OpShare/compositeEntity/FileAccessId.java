package com.example.OpShare.compositeEntity;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileAccessId implements Serializable {
    @Column(name = "file_id")
    private Long fileId;
    @Column(name = "user_id")
    private Long userId;
}