// ---------- SastScanner.java ----------
package com.example.securityservice.service;


import com.example.securityservice.entities.ScanType;
import com.example.securityservice.entities.SeverityLevel;
import com.example.securityservice.entities.Vulnerability;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SastScanner {
    public List<Vulnerability> scan(Long projectId) {
        // TODO: integrate real Bandit / SonarQube call here

        return List.of(
                Vulnerability.builder()
                        .type(ScanType.SAST)              // FIX: enum not String
                        .severity(SeverityLevel.HIGH)      // FIX: enum not String
                        .description("SQL Injection risk in UserService")
                        .filePath("UserService.java")
                        .build()
        );
    }
}
