package com.example.securityservice.controller;

import com.example.securityservice.entities.*;
import com.example.securityservice.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    @PostMapping("/scan")
    public SecurityScan scan(@RequestParam Long executionId,
                             @RequestParam ScanType type) {
        return securityService.performScan(executionId, type);
    }

    @GetMapping("/execution/{id}")
    public List<SecurityScan> getScans(@PathVariable Long id) {
        return securityService.getScansByExecution(id);
    }
}