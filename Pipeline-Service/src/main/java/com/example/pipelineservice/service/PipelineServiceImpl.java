package com.example.pipelineservice.service;

import com.example.pipelineservice.MAPPER.PipelineMapper;
import com.example.pipelineservice.client.dto.request.CreatePipelineRequest;
import com.example.pipelineservice.client.dto.response.PipelineResponse;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Project;
import com.example.pipelineservice.repository.PipelineRepository;
import com.example.pipelineservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository pipelineRepository;
    private final ProjectRepository projectRepository;
    private final PipelineMapper pipelineMapper;

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void checkOwnership(Project project) {
        if (!isAdmin() && !project.getOwner().equals(getCurrentUsername())) {
            throw new RuntimeException("Access denied");
        }
    }

    // ================= CREATE =================
    @Override
    public PipelineResponse createPipeline(Long projectId, CreatePipelineRequest request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        checkOwnership(project);

        Pipeline pipeline = pipelineMapper.toEntity(request, project);
        pipeline.setCreatedAt(LocalDateTime.now());

        return pipelineMapper.toResponse(pipelineRepository.save(pipeline));
    }

    // ================= GET =================
    @Override
    public List<PipelineResponse> getPipelinesByProject(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        checkOwnership(project);

        return pipelineRepository.findByProject(project)
                .stream()
                .map(pipelineMapper::toResponse)
                .toList();
    }

    @Override
    public PipelineResponse getPipelineById(Long pipelineId) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new RuntimeException("Pipeline not found"));

        checkOwnership(pipeline.getProject());

        return pipelineMapper.toResponse(pipeline);
    }

    @Override
    public PipelineResponse updatePipeline(Long pipelineId, CreatePipelineRequest request) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new RuntimeException("Pipeline not found"));

        checkOwnership(pipeline.getProject());

        pipeline.setName(request.getName());

        return pipelineMapper.toResponse(pipelineRepository.save(pipeline));
    }

    @Override
    public void deletePipeline(Long pipelineId) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new RuntimeException("Pipeline not found"));

        checkOwnership(pipeline.getProject());

        pipelineRepository.delete(pipeline);
    }
}