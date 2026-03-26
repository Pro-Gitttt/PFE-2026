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
        // TODO: integrate real Trivy / Snyk call here
        return List.of(
                Vulnerability.builder()
                        .type(ScanType.SCA)
                        .severity(SeverityLevel.CRITICAL)
                        .description("Vulnerable dependency: log4j 2.14.1")
                        .cve("CVE-2021-44228")
                        .build()
        );
    }
}
