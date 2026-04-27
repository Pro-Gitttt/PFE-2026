// ---------- SecurityServiceImpl.java ----------
package com.example.securityservice.service;

import com.example.securityservice.dto.ScanDetailResponse;
import com.example.securityservice.dto.SecurityScanResponse;
import com.example.securityservice.entities.SecurityScan;
import com.example.securityservice.entities.Vulnerability;
import com.example.securityservice.mapper.SecurityMapper;
import com.example.securityservice.repository.SecurityScanRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SecurityServiceImpl implements SecurityService {

    private final SecurityScanRepository scanRepository;
    private final SecurityMapper securityMapper;

    private final TrivyParser trivyParser;
    private final GitleaksParser gitleaksParser;
    private final PolicyService policyService;

    // =========================
    // MAIN SCAN
    // =========================
    @Override
    public SecurityScanResponse scan(
            Long executionId,
            Long projectId,
            MultipartFile trivy,
            MultipartFile gitleaks
    ) {

        // 1. Parse real reports
        List<Vulnerability> vulnerabilities = new ArrayList<>();

        vulnerabilities.addAll(trivyParser.parse(trivy));
        vulnerabilities.addAll(gitleaksParser.parse(gitleaks));

        // 2. Score
        double score = calculateScore(vulnerabilities);

        // 3. Policy
        boolean blocked = policyService.shouldBlock(score, vulnerabilities);

        // 4. Build entity
        SecurityScan scan = SecurityScan.builder()
                .executionId(executionId)
                .projectId(projectId)
                .score(score)
                .blocked(blocked)
                .createdAt(LocalDateTime.now())
                .build();

        vulnerabilities.forEach(v -> v.setScan(scan));
        scan.setVulnerabilities(vulnerabilities);

        SecurityScan saved = scanRepository.save(scan);

        return securityMapper.toResponse(saved);
    }

    // =========================
    // GET BY EXECUTION
    // =========================
    @Override
    public ScanDetailResponse getScanByExecution(Long executionId) {

        SecurityScan scan = scanRepository
                .findTopByExecutionIdOrderByCreatedAtDesc(executionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("No scan for execution: " + executionId));

        return securityMapper.toDetailResponse(scan);
    }

    // =========================
    // GET BY PROJECT
    // =========================
    @Override
    public List<ScanDetailResponse> getScansByProject(Long projectId) {

        return scanRepository.findByProjectId(projectId)
                .stream()
                .map(securityMapper::toDetailResponse)
                .toList();
    }

    // =========================
    // SCORE LOGIC
    // =========================
    private double calculateScore(List<Vulnerability> vulnerabilities) {

        double score = 100.0;

        for (Vulnerability v : vulnerabilities) {
            if (v.getSeverity() == null) continue;

            switch (v.getSeverity()) {
                case CRITICAL -> score -= 30;
                case HIGH -> score -= 20;
                case MEDIUM -> score -= 10;
                case LOW -> score -= 5;
            }
        }

        return Math.max(score, 0);
    }
}