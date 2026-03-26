// ---------- SecurityController.java ----------
package com.example.securityservice.controller;

import com.example.securityservice.dto.ScanDetailResponse;
import com.example.securityservice.dto.SecurityScanRequest;
import com.example.securityservice.dto.SecurityScanResponse;
import com.example.securityservice.service.SecurityService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    // Called by pipeline-service via Feign
    @PostMapping("/scan")
    @ResponseStatus(HttpStatus.CREATED)
    public SecurityScanResponse scan(@RequestBody SecurityScanRequest request) {
        return securityService.scan(request);
    }

    // Called by dashboard / chatbot — "give me the security report for execution X"
    @GetMapping("/scan/execution/{executionId}")
    public ScanDetailResponse getScanByExecution(@PathVariable Long executionId) {
        return securityService.getScanByExecution(executionId);
    }

    // Called by dashboard — history of all scans for a project
    @GetMapping("/scan/project/{projectId}")
    public List<ScanDetailResponse> getScansByProject(@PathVariable Long projectId) {
        return securityService.getScansByProject(projectId);
    }
}
 