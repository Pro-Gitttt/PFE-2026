// ---------- ProjectRepository.java ----------
package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByName(String name);
    boolean existsByName(String name);
    Optional<Project> findByRepositoryUrl(String repositoryUrl);
}