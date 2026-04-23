package com.example.pipelineservice.service;

import com.example.pipelineservice.MAPPER.ProjectMapper;
import com.example.pipelineservice.client.dto.request.CreateProjectRequest;
import com.example.pipelineservice.client.dto.response.ProjectResponse;
import com.example.pipelineservice.entities.Project;
import com.example.pipelineservice.exception.ResourceNotFoundException;
import com.example.pipelineservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    // ================= CREATE =================
    @Override
    public ProjectResponse createProject(CreateProjectRequest request,
                                         String username) {

        if (projectRepository.existsByName(request.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Project already exists");
        }

        Project project = projectMapper.toEntity(request, username);

        return projectMapper.toResponse(projectRepository.save(project));
    }

    // ================= GET =================
    @Override
    public List<ProjectResponse> getProjects(String username, String role) {

        if (isAdmin(role)) {
            return projectRepository.findAll()
                    .stream()
                    .map(projectMapper::toResponse)
                    .toList();
        }

        return projectRepository.findByCreatedBy(username)
                .stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    // ================= GET ONE =================
    @Override
    public ProjectResponse getProjectById(Long id,
                                          String username,
                                          String role) {

        Project project = findOrThrow(id);
        checkAccess(project, username, role);

        return projectMapper.toResponse(project);
    }

    // ================= UPDATE =================
    @Override
    public ProjectResponse updateProject(Long id,
                                         CreateProjectRequest request,
                                         String username,
                                         String role) {

        Project project = findOrThrow(id);
        checkAccess(project, username, role);

        project.setName(request.getName());
        project.setRepositoryUrl(request.getRepositoryUrl());
        project.setBranch(request.getBranch());

        return projectMapper.toResponse(projectRepository.save(project));
    }

    // ================= DELETE =================
    @Override
    public void deleteProject(Long id,
                              String username,
                              String role) {

        Project project = findOrThrow(id);
        checkAccess(project, username, role);

        projectRepository.delete(project);
    }

    // ================= HELPERS =================
    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    private void checkAccess(Project project, String username, String role) {
        if (isAdmin(role)) return;

        if (!project.getCreatedBy().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role)
                || "ROLE_ADMIN".equalsIgnoreCase(role);
    }
}