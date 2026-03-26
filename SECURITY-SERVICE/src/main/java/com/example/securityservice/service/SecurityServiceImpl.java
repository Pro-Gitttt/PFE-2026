// ---------- SecurityServiceImpl.java ----------
package com.example.securityservice.service;

import com.example.securityservice.dto.ScanDetailResponse;
import com.example.securityservice.dto.SecurityScanRequest;
import com.example.securityservice.dto.SecurityScanResponse;
import com.example.securityservice.entities.SecurityScan;
import com.example.securityservice.entities.SeverityLevel;
import com.example.securityservice.entities.Vulnerability;

import com.example.securityservice.mapper.SecurityMapper;
import com.example.securityservice.repository.SecurityScanRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SecurityServiceImpl implements SecurityService {

    private final ScanOrchestrator       scanOrchestrator;
    private final PolicyService          policyService;
    private final SecurityScanRepository scanRepository;
    private final SecurityMapper         securityMapper;

    // -------------------------------------------------------
    // MAIN SCAN — called by pipeline-service via Feign
    // -------------------------------------------------------

    @Override
    public SecurityScanResponse scan(SecurityScanRequest request) {

        // 1. Run all 3 scanners
        List<Vulnerability> vulnerabilities =
                scanOrchestrator.runAllScans(request.getProjectId());

        // 2. Calculate score (starts at 100, deduct per severity)
        double score = calculateScore(vulnerabilities);

        // 3. Policy enforcement
        boolean blocked = policyService.shouldBlock(score, vulnerabilities);

        // 4. Build entity
        SecurityScan scan = SecurityScan.builder()
                .projectId(request.getProjectId())
                .executionId(request.getExecutionId())
                .score(score)
                .blocked(blocked)
                .createdAt(LocalDateTime.now())
                .build();

        // 5. Link each vulnerability back to the scan
        vulnerabilities.forEach(v -> v.setScan(scan));
        scan.setVulnerabilities(vulnerabilities);

        // 6. Persist (cascade saves vulnerabilities automatically)
        SecurityScan saved = scanRepository.save(scan);

        // 7. Return minimal response to pipeline-service
        return securityMapper.toResponse(saved);
    }

    // -------------------------------------------------------
    // GET SCAN BY EXECUTION — for dashboard / chatbot
    // -------------------------------------------------------

    @Override
    public ScanDetailResponse getScanByExecution(Long executionId) {

        SecurityScan scan = scanRepository
                .findTopByExecutionIdOrderByCreatedAtDesc(executionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No scan found for executionId: " + executionId));

        return securityMapper.toDetailResponse(scan);
    }

    // -------------------------------------------------------
    // GET ALL SCANS FOR A PROJECT — for dashboard
    // -------------------------------------------------------

    @Override
    public List<ScanDetailResponse> getScansByProject(Long projectId) {

        return scanRepository.findByProjectId(projectId)
                .stream()
                .map(securityMapper::toDetailResponse)
                .toList();
    }

    // -------------------------------------------------------
    // SCORING LOGIC
    // -------------------------------------------------------

    private double calculateScore(List<Vulnerability> vulnerabilities) {

        double score = 100.0;

        for (Vulnerability v : vulnerabilities) {
            if (v.getSeverity() == null) continue;
            score -= switch (v.getSeverity()) {
                case CRITICAL -> 30;
                case HIGH     -> 20;
                case MEDIUM   -> 10;
                case LOW      ->  5;
            };
        }

        return Math.max(score, 0);
    }
}