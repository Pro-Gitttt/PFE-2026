// ---------- AuthServiceImpl.java ----------
package com.example.authservice.service;

import com.example.authservice.Dto.*;


import com.example.authservice.entities.User;

import com.example.authservice.exception.ResourceNotFoundException;
import com.example.authservice.repositories.UserRepository;
import com.example.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService      jwtService;

    // -------------------------------------------------------
    // REGISTER
    // -------------------------------------------------------

    @Override
    public AuthResponse register(RegisterRequest request) {

        // FIX: duplicate checks — original had none, causing raw DB constraint violations
        if (userRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Username already taken: " + request.getUsername());

        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email already registered: " + request.getEmail());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                // FIX: set createdAt explicitly (not inline on field — JPA timing issue)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // FIX: return both tokens + role, not just a single token
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    // -------------------------------------------------------
    // LOGIN
    // -------------------------------------------------------

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid credentials");

        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    // -------------------------------------------------------
    // REFRESH TOKEN
    // FIX: was completely missing — DTO existed but no logic
    // -------------------------------------------------------

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        // Validate it is actually a refresh token (not an access token reused)
        String type = jwtService.extractType(refreshToken);
        if (!"refresh".equals(type))
            throw new RuntimeException("Invalid token type — must be a refresh token");

        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate signature + expiry
        if (!jwtService.isTokenValid(refreshToken, user))
            throw new RuntimeException("Refresh token expired or invalid");

        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    // -------------------------------------------------------
    // GET CURRENT USER — for /me endpoint
    // -------------------------------------------------------

    @Override
    public UserResponse getCurrentUser(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
 
 