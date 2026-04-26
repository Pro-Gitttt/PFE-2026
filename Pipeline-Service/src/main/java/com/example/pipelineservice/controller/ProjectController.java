package com.example.pipelineservice.controller;

import com.example.pipelineservice.client.dto.request.CreateProjectRequest;
import com.example.pipelineservice.client.dto.response.ProjectResponse;
import com.example.pipelineservice.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pipeline/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // ─────────────────────────────
    // CREATE
    // ─────────────────────────────
    @PostMapping
    public ProjectResponse createProject(
            @RequestBody CreateProjectRequest request
    ) {
        return projectService.createProject(request);
    }

    // ─────────────────────────────
    // GET ALL
    // ─────────────────────────────
    @GetMapping
    public List<ProjectResponse> getProjects() {
        return projectService.getProjects();
    }

    // ─────────────────────────────
    // GET BY ID
    // ─────────────────────────────
    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    // ─────────────────────────────
    // UPDATE
    // ─────────────────────────────
    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable Long id,
            @RequestBody CreateProjectRequest request
    ) {
        return projectService.updateProject(id, request);
    }

    // ─────────────────────────────
    // DELETE (SOFT DELETE)
    // ─────────────────────────────
    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    // ─────────────────────────────
    // RESTORE (ADMIN ONLY)
    // ─────────────────────────────
    @PostMapping("/{id}/restore")
    public ProjectResponse restoreProject(@PathVariable Long id) {
        return projectService.restoreProject(id);
    }
}