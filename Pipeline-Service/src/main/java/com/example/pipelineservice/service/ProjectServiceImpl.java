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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    // ─────────────────────────────
    // CREATE
    // ─────────────────────────────
    @Override
    public ProjectResponse createProject(CreateProjectRequest request) {

        String username = getUsername();

        if (projectRepository.existsByName(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project already exists");
        }

        Project project = projectMapper.toEntity(request, username);

        return projectMapper.toResponse(projectRepository.save(project));
    }

    // ─────────────────────────────
    // GET ALL
    // ─────────────────────────────
    @Override
    public List<ProjectResponse> getProjects() {

        Authentication auth = getAuth();
        String username = auth.getName();

        if (isAdmin(auth)) {
            return projectRepository.findByDeletedFalse()
                    .stream().map(projectMapper::toResponse).toList();
        }

        return projectRepository.findByCreatedByAndDeletedFalse(username)
                .stream().map(projectMapper::toResponse).toList();
    }

    // ─────────────────────────────
    // GET BY ID
    // ─────────────────────────────
    @Override
    public ProjectResponse getProjectById(Long id) {

        Authentication auth = getAuth();
        String username = auth.getName();

        Project project = findOrThrow(id);

        if (!isAdmin(auth) && !project.getCreatedBy().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return projectMapper.toResponse(project);
    }

    // ─────────────────────────────
    // UPDATE
    // ─────────────────────────────
    @Override
    public ProjectResponse updateProject(Long id, CreateProjectRequest request) {

        Authentication auth = getAuth();
        String username = auth.getName();

        Project project = findOrThrow(id);

        if (!isAdmin(auth) && !project.getCreatedBy().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        project.setName(request.getName());
        project.setRepositoryUrl(request.getRepositoryUrl());
        project.setBranch(request.getBranch());

        if (request.getOwner() != null) {
            project.setOwner(request.getOwner());
        }

        if (request.getVcsType() != null) {
            project.setVcsType(request.getVcsType());
        }

        return projectMapper.toResponse(projectRepository.save(project));
    }

    // ─────────────────────────────
    // DELETE (SOFT DELETE)
    // ─────────────────────────────
    @Override
    public void deleteProject(Long id) {

        Authentication auth = getAuth();

        Project project = findOrThrow(id);

        if (!isAdmin(auth) && !project.getCreatedBy().equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        project.setDeleted(true);
        project.setDeletedAt(LocalDateTime.now());

        projectRepository.save(project);
    }

    // ─────────────────────────────
    // RESTORE (ADMIN ONLY)
    // ─────────────────────────────
    @Override
    public ProjectResponse restoreProject(Long id) {

        Authentication auth = getAuth();

        if (!isAdmin(auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }

        Project project = findOrThrow(id);

        project.setDeleted(false);
        project.setDeletedAt(null);

        return projectMapper.toResponse(projectRepository.save(project));
    }

    // ─────────────────────────────
    // HELPERS
    // ─────────────────────────────
    private Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private String getUsername() {
        return getAuth().getName();
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }
}