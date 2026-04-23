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

    @PostMapping
    public ProjectResponse createProject(
            @RequestBody CreateProjectRequest request,
            Authentication auth
    ) {
        return projectService.createProject(request, auth.getName());
    }

    @GetMapping
    public List<ProjectResponse> getProjects(Authentication auth) {
        String username = auth.getName();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        return projectService.getProjects(username, role);
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(
            @PathVariable Long id,
            Authentication auth
    ) {
        String username = auth.getName();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        return projectService.getProjectById(id, username, role);
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable Long id,
            @RequestBody CreateProjectRequest request,
            Authentication auth
    ) {
        String username = auth.getName();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        return projectService.updateProject(id, request, username, role);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(
            @PathVariable Long id,
            Authentication auth
    ) {
        String username = auth.getName();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        projectService.deleteProject(id, username, role);
    }
}