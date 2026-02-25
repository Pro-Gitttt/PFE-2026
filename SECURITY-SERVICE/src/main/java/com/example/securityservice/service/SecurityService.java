package com.example.securityservice.service;

import com.example.securityservice.entities.*;

import java.util.List;

public interface SecurityService {

    SecurityScan performScan(Long pipelineExecutionId, ScanType type);

    List<SecurityScan> getScansByExecution(Long executionId);

    List<Vulnerability> getVulnerabilities(Long scanId);

    Double calculateSecurityScore(Long scanId);

}