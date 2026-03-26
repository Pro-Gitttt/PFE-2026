package com.example.pipelineservice.entities;

import com.example.pipelineservice.entities.Pipeline;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String repositoryUrl;

    private String branch;

    private String owner;

    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    private VcsType vcsType;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<Pipeline> pipelines;
}