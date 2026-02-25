package com.example.authservice.service;


import com.example.authservice.Dto.AuthResponse;
import com.example.authservice.Dto.LoginRequest;
import com.example.authservice.Dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}