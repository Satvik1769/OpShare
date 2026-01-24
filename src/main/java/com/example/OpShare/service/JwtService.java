package com.example.OpShare.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-hours}")
    private int expirationHours;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String contactNumber, String deviceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("contactNumber", contactNumber);
        claims.put("deviceId", deviceId);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + (expirationHours * 60 * 60 * 1000L));

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getContactNumberFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("contactNumber", String.class);
    }

    public String getDeviceIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("deviceId", String.class);
    }

    public LocalDateTime getExpirationFromToken(String token) {
        Claims claims = extractAllClaims(token);
        Date expiration = claims.getExpiration();
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public int getExpirationHours() {
        return expirationHours;
    }
}