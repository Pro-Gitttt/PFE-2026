package com.example.pipelineservice.MAPPER;

import com.example.pipelineservice.client.dto.request.CreateProjectRequest;
import com.example.pipelineservice.client.dto.response.ProjectResponse;
import com.example.pipelineservice.entities.Project;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProjectMapper {

    public Project toEntity(CreateProjectRequest request) {

        return Project.builder()
                .name(request.getName())
                .repositoryUrl(request.getRepositoryUrl())
                .branch(request.getBranch())
                // FIX: vcsType was never set — every project was saved with null vcsType
                .vcsType(request.getVcsType())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public ProjectResponse toResponse(Project project) {

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .repositoryUrl(project.getRepositoryUrl())
                .branch(project.getBranch())
                // FIX: also include vcsType in response so frontend knows GitHub vs GitLab
                .vcsType(project.getVcsType())
                .createdAt(project.getCreatedAt())
                .build();
    }
}