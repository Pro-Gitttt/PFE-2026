// ---------- PolicyService.java ----------
package com.example.securityservice.service;

import com.example.securityservice.entities.SeverityLevel;
import com.example.securityservice.entities.Vulnerability;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyService {

    private static final double MIN_SCORE = 70.0;

    // Block if score too low OR any CRITICAL vulnerability exists
    public boolean shouldBlock(double score, List<Vulnerability> vulnerabilities) {

        boolean hasCritical = vulnerabilities.stream()
                .anyMatch(v -> v.getSeverity() == SeverityLevel.CRITICAL);

        return score < MIN_SCORE || hasCritical;
    }
}
 