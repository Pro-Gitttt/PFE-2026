// ---------- JwtService.java ----------
// FIX 1: secret moved to application.properties via @Value — never hardcode secrets
// FIX 2: updated to non-deprecated JJWT 0.12+ API (Jwts.builder().subject() etc.)
// FIX 3: generateRefreshToken() added — was completely missing
package com.example.authservice.security;

import com.example.authservice.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // FIX: read from application.properties — add this line:
    //   jwt.secret=your-very-long-secret-key-at-least-256-bits-long
    @Value("${jwt.secret}")
    private String secret;

    // Access token: 24 hours
    private static final long ACCESS_TOKEN_EXPIRY  = 86_400_000L;
    // Refresh token: 7 days
    private static final long REFRESH_TOKEN_EXPIRY = 604_800_000L;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
                .signWith(getKey())
                .compact();
    }

    // FIX: refresh token generation — was missing entirely
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}