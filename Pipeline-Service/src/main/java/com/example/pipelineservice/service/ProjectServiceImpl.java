package com.example.pipelineservice.service;


import com.example.pipelineservice.MAPPER.ProjectMapper;
import com.example.pipelineservice.client.dto.request.CreateProjectRequest;
import com.example.pipelineservice.client.dto.response.ProjectResponse;
import com.example.pipelineservice.entities.Project;

import com.example.pipelineservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Override
    public ProjectResponse createProject(CreateProjectRequest request) {

        if (projectRepository.existsByName(request.getName())) {
            throw new RuntimeException("Project name already exists");
        }

        Project project = projectMapper.toEntity(request);

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Override
    public List<ProjectResponse> getAllProjects() {

        return projectRepository.findAll()
                .stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    public ProjectResponse getProjectById(Long id) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Project not found with id: " + id));

        return projectMapper.toResponse(project);
    }

    @Override
    public ProjectResponse updateProject(Long id, CreateProjectRequest request) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Project not found with id: " + id));

        project.setName(request.getName());
        project.setRepositoryUrl(request.getRepositoryUrl());
        project.setBranch(request.getBranch());

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Override
    public void deleteProject(Long id) {

        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found with id: " + id);
        }

        projectRepository.deleteById(id);
    }
}