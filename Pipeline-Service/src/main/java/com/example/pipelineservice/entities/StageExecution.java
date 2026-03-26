package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pipeline_execution_id")
    private PipelineExecution pipelineExecution;

    @ManyToOne
    @JoinColumn(name = "stage_id")
    private Stage stage;   // ✅ THIS MUST EXIST

    @Enumerated(EnumType.STRING)
    private PipelineStatus status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}