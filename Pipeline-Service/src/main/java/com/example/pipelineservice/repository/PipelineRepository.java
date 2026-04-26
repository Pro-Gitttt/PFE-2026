package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PipelineRepository extends JpaRepository<Pipeline, Long> {
    List<Pipeline> findByProject(Project project);
    Optional<Pipeline> findFirstByProject(Project project);
    Optional<Pipeline> findByJenkinsJobName(String jenkinsJobName);
}