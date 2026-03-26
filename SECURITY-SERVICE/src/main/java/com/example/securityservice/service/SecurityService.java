// ---------- SecurityService.java (interface) ----------
package com.example.securityservice.service;

import com.example.securityservice.dto.ScanDetailResponse;
import com.example.securityservice.dto.SecurityScanRequest;
import com.example.securityservice.dto.SecurityScanResponse;

import java.util.List;

public interface SecurityService {

    SecurityScanResponse scan(SecurityScanRequest request);

    ScanDetailResponse getScanByExecution(Long executionId);

    List<ScanDetailResponse> getScansByProject(Long projectId);
}