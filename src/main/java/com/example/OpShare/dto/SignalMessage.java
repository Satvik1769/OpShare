package com.example.OpShare.dto;

import lombok.Data;

@Data
public class SignalMessage {
    private String type;      // "offer" | "answer" | "ice-candidate" | "ready"
    private String from;      // sender's userId (set server-side)
    private String to;        // recipient's userId
    private String sdp;       // for offer/answer
    private String candidate; // for ICE candidate
    private String sdpMid;    // ICE candidate sdpMid
    private Integer sdpMLineIndex; // ICE candidate line index
}
