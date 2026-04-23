package com.example.pipelineservice.service;

import com.example.pipelineservice.client.dto.request.CreateProjectRequest;
import com.example.pipelineservice.client.dto.response.ProjectResponse;

import java.util.List;

public interface ProjectService {

    /**
     * Creates a project. createdBy is set to the caller's username.
     *
     * @param request   — validated request body
     * @param username  — extracted from JWT by controller
     */
    ProjectResponse createProject(CreateProjectRequest request, String username);

    /**
     * Returns:
     *  - All projects   → if role == "ADMIN"
     *  - Only own projects → if any other role
     *
     * @param username — caller's username from JWT
     * @param role     — caller's role from JWT ("ADMIN", "DEV", "DEVOPS", "AUDITOR")
     */
    List<ProjectResponse> getProjects(String username, String role);

    /**
     * Get single project by ID.
     * Throws 403 if caller is not ADMIN and does not own the project.
     */
    ProjectResponse getProjectById(Long id, String username, String role);

    /**
     * Update project.
     * Only the owner or ADMIN can update.
     */
    ProjectResponse updateProject(Long id, CreateProjectRequest request,
                                  String username, String role);

    /**
     * Delete project.
     * Only the owner or ADMIN can delete.
     */
    void deleteProject(Long id, String username, String role);
}
