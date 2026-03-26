package com.example.pipelineservice.service;

import com.example.pipelineservice.MAPPER.PipelineMapper;
import com.example.pipelineservice.client.dto.request.CreatePipelineRequest;
import com.example.pipelineservice.client.dto.response.PipelineResponse;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Project;
import com.example.pipelineservice.repository.PipelineRepository;
import com.example.pipelineservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
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

    // ================================
    // CREATE PIPELINE
    // ================================

    @Override
    public PipelineResponse createPipeline(Long projectId, CreatePipelineRequest request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Pipeline pipeline = pipelineMapper.toEntity(request, project);

        pipeline.setStatus(null);
        pipeline.setCreatedAt(LocalDateTime.now());

        Pipeline savedPipeline = pipelineRepository.save(pipeline);

        return pipelineMapper.toResponse(savedPipeline);
    }

    // ================================
    // GET PIPELINES BY PROJECT
    // ================================

    @Override
    public List<PipelineResponse> getPipelinesByProject(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        return pipelineRepository.findByProject(project)
                .stream()
                .map(pipelineMapper::toResponse)
                .toList();
    }

    // ================================
    // GET PIPELINE BY ID
    // ================================

    @Override
    public PipelineResponse getPipelineById(Long pipelineId) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new RuntimeException("Pipeline not found"));

        return pipelineMapper.toResponse(pipeline);
    }

    // ================================
    // UPDATE PIPELINE
    // ================================

    @Override
    public PipelineResponse updatePipeline(Long pipelineId, CreatePipelineRequest request) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new RuntimeException("Pipeline not found"));

        pipeline.setName(request.getName());

        Pipeline updatedPipeline = pipelineRepository.save(pipeline);

        return pipelineMapper.toResponse(updatedPipeline);
    }

    // ================================
    // DELETE PIPELINE
    // ================================

    @Override
    public void deletePipeline(Long pipelineId) {

        if (!pipelineRepository.existsById(pipelineId)) {
            throw new RuntimeException("Pipeline not found");
        }

        pipelineRepository.deleteById(pipelineId);
    }
}