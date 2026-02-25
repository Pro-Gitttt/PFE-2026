package com.example.securityservice.repository;

import com.example.securityservice.entities.SecurityScan;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface SecurityScanRepository extends JpaRepository<SecurityScan, Long> {
    List<SecurityScan> findByPipelineExecutionId(Long pipelineExecutionId);
}