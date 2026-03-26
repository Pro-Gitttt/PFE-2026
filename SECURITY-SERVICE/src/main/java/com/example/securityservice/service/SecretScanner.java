// ---------- SecretScanner.java ----------
package com.example.securityservice.service;

import com.example.securityservice.entities.ScanType;
import com.example.securityservice.entities.SeverityLevel;
import com.example.securityservice.entities.Vulnerability;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecretScanner {

    public List<Vulnerability> scan(Long projectId) {
        // TODO: integrate real Gitleaks call here
        return List.of(
                Vulnerability.builder()
                        .type(ScanType.SECRET)
                        .severity(SeverityLevel.HIGH)
                        .description("Hardcoded AWS API key detected")
                        .filePath("src/main/resources/config.yml")
                        .build()
        );
    }
}