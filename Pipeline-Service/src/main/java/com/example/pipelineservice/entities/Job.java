package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private PipelineStatus status;

    private LocalDateTime createdAt;

    // ✅ RELATION ONLY (NO stageId field)
    @ManyToOne
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;
}