package com.example.OpShare.dto;

import lombok.Data;

@Data
public class SignalMessage {
    private String type;   // "offer" | "answer" | "ice"
    private String from;   // sender's userId
    private String sdp;    // for offer/answer
    private String candidate; // for ICE
}
