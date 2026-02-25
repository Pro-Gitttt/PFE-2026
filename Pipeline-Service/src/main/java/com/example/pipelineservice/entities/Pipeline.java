package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pipeline")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;

    private String name;

    @Enumerated(EnumType.STRING)
    private PipelineStatus status;

    private LocalDateTime createdAt;

    // 🔁 One pipeline → many stages
    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL)
    private List<Stage> stages;

    // 🔁 One pipeline → many executions
    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL)
    private List<PipelineExecution> executions;
}