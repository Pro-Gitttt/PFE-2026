package com.example.pipelineservice.entities;

import com.example.pipelineservice.entities.Job;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.StageType;
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
@Data
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int orderIndex;

    @Enumerated(EnumType.STRING)
    private StageType type;

    @ManyToOne
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL)
    private List<Job> jobs;
}