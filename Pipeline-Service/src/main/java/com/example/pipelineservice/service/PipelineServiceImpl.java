package com.example.pipelineservice.service;

import com.example.pipelineservice.MAPPER.PipelineMapper;
import com.example.pipelineservice.client.dto.request.CreatePipelineRequest;
import com.example.pipelineservice.client.dto.response.PipelineResponse;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Project;
import com.example.pipelineservice.exception.ResourceNotFoundException;
import com.example.pipelineservice.repository.PipelineRepository;
import com.example.pipelineservice.repository.ProjectRepository;
import com.example.pipelineservice.security.AuthorizationHelper;
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
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository pipelineRepository;
    private final ProjectRepository  projectRepository;
    private final PipelineMapper     pipelineMapper;
    private final AuthorizationHelper auth;   // ← injected helper

    @Override
    public PipelineResponse createPipeline(Long projectId, CreatePipelineRequest request) {
        Project project = findProjectOrThrow(projectId);
        assertProjectAccess(project);

        Pipeline pipeline = pipelineMapper.toEntity(request, project);
        return pipelineMapper.toResponse(pipelineRepository.save(pipeline));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PipelineResponse> getPipelinesByProject(Long projectId) {
        Project project = findProjectOrThrow(projectId);
        assertProjectAccess(project);
        return pipelineRepository.findByProject(project)
                .stream().map(pipelineMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PipelineResponse getPipelineById(Long pipelineId) {
        Pipeline pipeline = findPipelineOrThrow(pipelineId);
        assertProjectAccess(pipeline.getProject());
        return pipelineMapper.toResponse(pipeline);
    }

    @Override
    public PipelineResponse updatePipeline(Long pipelineId, CreatePipelineRequest request) {
        Pipeline pipeline = findPipelineOrThrow(pipelineId);
        assertProjectAccess(pipeline.getProject());

        pipeline.setName(request.getName());
        pipeline.setJenkinsJobName(request.getJenkinsJobName());
        return pipelineMapper.toResponse(pipelineRepository.save(pipeline));
    }

    @Override
    public void deletePipeline(Long pipelineId) {
        Pipeline pipeline = findPipelineOrThrow(pipelineId);
        assertProjectAccess(pipeline.getProject());
        pipelineRepository.delete(pipeline);
    }

    // ── helpers ───────────────────────────────────────────────

    private Project findProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    private Pipeline findPipelineOrThrow(Long id) {
        return pipelineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found: " + id));
    }

    /**
     * THE ORIGINAL BUG WAS HERE.
     *
     * Old code (broken):
     * ─────────────────────────────────────────────────────────────
     *   boolean isAdmin = auth.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
     *   if (!isAdmin && !project.getCreatedBy().equals(username)) { → FORBIDDEN }
     * ─────────────────────────────────────────────────────────────
     * Bug: DEVOPS was never checked → always hit the ownership check → always FORBIDDEN
     * unless the DEVOPS user happened to be the project creator.
     *
     * Fixed code:
     * ─────────────────────────────────────────────────────────────
     *   ADMIN   → pass (global access)
     *   DEVOPS  → pass (global access by role)
     *   DEV     → pass only if project.createdBy == currentUser
     *   AUDITOR → pass only if project.createdBy == currentUser
     * ─────────────────────────────────────────────────────────────
     */
    private void assertProjectAccess(Project project) {
        if (!auth.canAccessProject(project.getCreatedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: you do not have permission to access this project.");
        }
    }
}