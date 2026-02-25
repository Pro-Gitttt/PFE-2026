package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "stage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // 🔗 Many stages → one pipeline
    @ManyToOne
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    // 🔁 One stage → many jobs
    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL)
    private List<Job> jobs;
}