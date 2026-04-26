package com.example.pipelineservice.service;

import com.example.pipelineservice.ExecutionTrackingService;
import com.example.pipelineservice.MAPPER.ExecutionMapper;
import com.example.pipelineservice.client.JenkinsClient;
import com.example.pipelineservice.client.dto.request.ExecutionRequest;
import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.entities.*;
import com.example.pipelineservice.exception.ResourceNotFoundException;
import com.example.pipelineservice.repository.PipelineExecutionRepository;
import com.example.pipelineservice.repository.PipelineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExecutionServiceImpl implements ExecutionService {

    private final PipelineRepository pipelineRepository;
    private final PipelineExecutionRepository executionRepository;
    private final ExecutionMapper executionMapper;
    private final JenkinsClient jenkinsClient;
    private final ExecutionTrackingService trackingService;

    @Override
    public ExecutionResponse executePipeline(Long pipelineId, ExecutionRequest request) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found"));

        // 1. CREATE EXECUTION
        PipelineExecution execution = PipelineExecution.builder()
                .pipeline(pipeline)
                .triggeredBy(request.getUserId())
                .commitHash(request.getCommitHash())
                .status(PipelineStatus.PENDING)
                .startTime(LocalDateTime.now())
                .build();

        execution = executionRepository.save(execution);

        // 🔥 VERY IMPORTANT → avoid async race condition
        executionRepository.flush();

        // 2. TRIGGER JENKINS
        String queueUrl = jenkinsClient.triggerJob(
                pipeline.getJenkinsJobName(),
                execution
        );

        String queueId = jenkinsClient.extractQueueId(queueUrl);

        execution.setJenkinsQueueId(Long.parseLong(queueId));
        executionRepository.save(execution);

        // 3. ASYNC TRACKING
        trackingService.trackAsync(
                execution.getId(),
                pipeline.getId(),
                queueId
        );

        // 4. RETURN IMMEDIATELY
        return executionMapper.toResponse(execution, List.of());
    }

    @Override
    public List<ExecutionResponse> getExecutionsByPipeline(Long pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found"));

        return executionRepository.findByPipelineOrderByStartTimeDesc(pipeline)
                .stream()
                .map(exec -> executionMapper.toResponse(exec, List.of()))
                .toList();
    }

    @Override
    public ExecutionResponse getExecutionById(Long executionId) {
        PipelineExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        return executionMapper.toResponse(execution, List.of());
    }

    @Override
    public void updateStatus(Long executionId, PipelineStatus status) {
        PipelineExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        execution.setStatus(status);
        executionRepository.save(execution);
    }
}