package com.example.securityservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "security_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pipelineExecutionId;

    @Enumerated(EnumType.STRING)
    private ScanType scanType;

    private Double securityScore;

    private Boolean blocked;

    private LocalDateTime scanDate;
}