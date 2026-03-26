package com.example.pipelineservice.controller;


import com.example.pipelineservice.client.dto.request.CreateProjectRequest;
import com.example.pipelineservice.client.dto.response.ProjectResponse;
import com.example.pipelineservice.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // ================= CREATE =================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    // ================= GET ALL =================

    @GetMapping
    public List<ProjectResponse> getAllProjects() {
        return projectService.getAllProjects();
    }

    // ================= GET BY ID =================

    @GetMapping("/{id}")
    public ProjectResponse getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    // ================= UPDATE =================

    @PutMapping("/{id}")
    public ProjectResponse updateProject(@PathVariable Long id,
                                         @RequestBody CreateProjectRequest request) {
        return projectService.updateProject(id, request);
    }

    // ================= DELETE =================

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }
}