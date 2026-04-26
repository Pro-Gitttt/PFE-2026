package com.example.pipelineservice.service;

import com.example.pipelineservice.MAPPER.PipelineMapper;
import com.example.pipelineservice.client.dto.request.CreatePipelineRequest;
import com.example.pipelineservice.client.dto.response.PipelineResponse;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Project;
import com.example.pipelineservice.exception.ResourceNotFoundException;
import com.example.pipelineservice.repository.PipelineRepository;
import com.example.pipelineservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Override
    public PipelineResponse createPipeline(Long projectId, CreatePipelineRequest request) {
        Project project = findProjectOrThrow(projectId);
        assertOwnership(project);

        Pipeline pipeline = pipelineMapper.toEntity(request, project);
        return pipelineMapper.toResponse(pipelineRepository.save(pipeline));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PipelineResponse> getPipelinesByProject(Long projectId) {
        Project project = findProjectOrThrow(projectId);
        assertOwnership(project);
        return pipelineRepository.findByProject(project)
                .stream().map(pipelineMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PipelineResponse getPipelineById(Long pipelineId) {
        Pipeline pipeline = findPipelineOrThrow(pipelineId);
        assertOwnership(pipeline.getProject());
        return pipelineMapper.toResponse(pipeline);
    }

    @Override
    public PipelineResponse updatePipeline(Long pipelineId, CreatePipelineRequest request) {
        Pipeline pipeline = findPipelineOrThrow(pipelineId);
        assertOwnership(pipeline.getProject());

        pipeline.setName(request.getName());
        pipeline.setJenkinsJobName(request.getJenkinsJobName());
        return pipelineMapper.toResponse(pipelineRepository.save(pipeline));
    }

    @Override
    public void deletePipeline(Long pipelineId) {
        Pipeline pipeline = findPipelineOrThrow(pipelineId);
        assertOwnership(pipeline.getProject());
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

    private void assertOwnership(Project project) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !project.getCreatedBy().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: you do not own this project.");
        }
    }
}