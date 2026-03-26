package com.example.pipelineservice.entities;

import com.example.pipelineservice.entities.Job;
import com.example.pipelineservice.entities.PipelineStatus;
import com.example.pipelineservice.entities.StageExecution;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JoinColumn(name = "stage_execution_id")
    private StageExecution stageExecution;

    @Enumerated(EnumType.STRING)
    private PipelineStatus status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}