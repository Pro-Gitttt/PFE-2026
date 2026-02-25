package com.example.pipelineservice.client;

import com.example.pipelineservice.client.dto.SecurityScanResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "security-service")
public interface SecurityClient {

    @PostMapping("/api/security/scan")
    SecurityScanResponse performScan(
            @RequestParam Long executionId,
            @RequestParam String type
    );
}