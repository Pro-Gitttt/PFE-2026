// ---------- ScaScanner.java ----------
package com.example.securityservice.service;
import com.example.securityservice.entities.ScanType;
import com.example.securityservice.entities.SeverityLevel;
import com.example.securityservice.entities.Vulnerability;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScaScanner {
    public List<Vulnerability> scan(Long projectId) {


        return List.of(
                Vulnerability.builder()
                        .type(ScanType.SCA)
                        .severity(SeverityLevel.CRITICAL)  // FIX: enum not String
                        .description("Vulnerable dependency: log4j")
                        .cve("CVE-2021-44228")
                        .build()
        );
    }
}