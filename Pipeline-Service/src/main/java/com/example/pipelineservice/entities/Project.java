package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "project")           // explicit table name avoids reserved-word issues
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String repositoryUrl;

    private String branch;

    /**
     * Optional free-text owner label (team name, description, etc.)
     * This is different from createdBy — owner is user-supplied text.
     */
    private String owner;

    /**
     * ★ NEW — The username extracted from the JWT.
     * Set automatically in ProjectServiceImpl.createProject().
     * Never comes from the HTTP request body.
     */
    @Column(nullable = false)
    private String createdBy;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private VcsType vcsType;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pipeline> pipelines;
}