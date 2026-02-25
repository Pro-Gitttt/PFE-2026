package com.example.authservice.Dto;

import com.example.authservice.entities.Role;
import lombok.Data;

@Data
public class RegisterRequest {

    private String username;
    private String email;
    private String password;
    private Role role;
}