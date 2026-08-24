package com.bluewave.apexbank.utils.services;

import com.bluewave.apexbank.users.Users;
import com.bluewave.apexbank.utils.common.SecurityUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final SecurityUtils securityUtils;


    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSignedKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignedKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Generates a short-lived (15-min) Access Token containing user roles.
     */
    public String generateAccessToken(Authentication authentication) {

        String username = authentication.getName();

        Users user = securityUtils.getUserByUsername(username);

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getAppRole().name())
                .toList();

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
                .signWith(getSignedKey())
                .compact();
    }

    /**
     * Generates a long-lived (7-day) Refresh Token containing username and unique sessionId.
     */
    public String generateRefreshToken(String username, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("sessionId", sessionId) // 🔑 Embedded Session ID claim
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(7, ChronoUnit.DAYS)))
                .signWith(getSignedKey())
                .compact();
    }

    /**
     * Extracts the custom 'sessionId' claim from a Refresh Token.
     */
    public String extractSessionIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("sessionId", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsernameFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        List<String> roles = claims.get("roles", List.class);
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
}