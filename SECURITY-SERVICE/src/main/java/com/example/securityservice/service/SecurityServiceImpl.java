package com.example.securityservice.service;

import com.example.securityservice.entities.*;
import com.example.securityservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private final SecurityScanRepository scanRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    @Override
    public SecurityScan performScan(Long pipelineExecutionId, ScanType type) {

        // Simulate vulnerabilities detection
        Random random = new Random();
        double score = 50 + random.nextDouble() * 50;

        SecurityScan scan = SecurityScan.builder()
                .pipelineExecutionId(pipelineExecutionId)
                .scanType(type)
                .securityScore(score)
                .blocked(score < 60)
                .scanDate(LocalDateTime.now())
                .build();

        return scanRepository.save(scan);
    }

    @Override
    public List<SecurityScan> getScansByExecution(Long executionId) {
        return scanRepository.findByPipelineExecutionId(executionId);
    }

    @Override
    public List<Vulnerability> getVulnerabilities(Long scanId) {
        return vulnerabilityRepository.findByScanId(scanId);
    }

    @Override
    public Double calculateSecurityScore(Long scanId) {

        List<Vulnerability> vulnerabilities =
                vulnerabilityRepository.findByScanId(scanId);

        long critical = vulnerabilities.stream()
                .filter(v -> v.getSeverity() == SeverityLevel.CRITICAL)
                .count();

        double score = 100.0 - (critical * 10.0);

        return score;
    }
}