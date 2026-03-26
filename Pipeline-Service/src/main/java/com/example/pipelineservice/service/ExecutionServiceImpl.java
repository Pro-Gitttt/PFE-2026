// ---------- ExecutionServiceImpl.java ----------
package com.example.pipelineservice.service;

import com.example.pipelineservice.MAPPER.ExecutionMapper;
import com.example.pipelineservice.client.JenkinsClient;
import com.example.pipelineservice.client.SecurityClient;
import com.example.pipelineservice.client.dto.request.ExecutionRequest;
import com.example.pipelineservice.client.dto.request.SecurityScanRequest;
import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.client.dto.response.SecurityScanResponse;
import com.example.pipelineservice.entities.*;
import com.example.pipelineservice.exception.ResourceNotFoundException;
import com.example.pipelineservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExecutionServiceImpl implements ExecutionService {

    private final PipelineRepository          pipelineRepository;
    private final PipelineExecutionRepository executionRepository;
    private final StageRepository             stageRepository;
    private final StageExecutionRepository    stageExecutionRepository;
    // FIX: inject ExecutionMapper — was missing, causing stages list to always be null
    private final ExecutionMapper             executionMapper;
    private final JenkinsClient              jenkinsClient;
    private final SecurityClient             securityClient;

    private static final double SECURITY_THRESHOLD = 70.0;

    @Override
    public ExecutionResponse executePipeline(Long pipelineId, ExecutionRequest request) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found"));

        pipeline.setStatus(PipelineStatus.RUNNING);

        PipelineExecution execution = PipelineExecution.builder()
                .pipeline(pipeline)
                .triggeredBy(request.getUserId())
                .commitHash(request.getCommitHash())
                .status(PipelineStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .build();

        execution = executionRepository.save(execution);

        List<Stage> stages = stageRepository.findByPipelineOrderByOrderIndexAsc(pipeline);

        for (Stage stage : stages) {

            StageExecution stageExec = StageExecution.builder()
                    .pipelineExecution(execution)
                    .stage(stage)
                    .status(PipelineStatus.RUNNING)
                    .startTime(LocalDateTime.now())
                    .build();

            stageExec = stageExecutionRepository.save(stageExec);

            boolean success = executeStage(stage, execution);

            stageExec.setEndTime(LocalDateTime.now());

            if (!success) {
                stageExec.setStatus(PipelineStatus.FAILED);
                execution.setStatus(PipelineStatus.FAILED);
                pipeline.setStatus(PipelineStatus.FAILED);
                stageExecutionRepository.save(stageExec);
                break;
            }

            stageExec.setStatus(PipelineStatus.SUCCESS);
            stageExecutionRepository.save(stageExec);
        }

        execution.setEndTime(LocalDateTime.now());

        if (execution.getStatus() != PipelineStatus.FAILED) {
            execution.setStatus(PipelineStatus.SUCCESS);
            pipeline.setStatus(PipelineStatus.SUCCESS);
        }

        pipelineRepository.save(pipeline);
        execution = executionRepository.save(execution);

        // FIX: load stage executions so the response actually contains them
        List<StageExecution> stageExecutions =
                stageExecutionRepository.findByPipelineExecution(execution);

        return executionMapper.toResponse(execution, stageExecutions);
    }

    private boolean executeStage(Stage stage, PipelineExecution execution) {
        return switch (stage.getType()) {
            case BUILD, TEST   -> executeJenkinsStage(execution);
            case SECURITY_SCAN -> executeSecurityStage(execution);
            case DEPLOY        -> true;
        };
    }

    private boolean executeJenkinsStage(PipelineExecution execution) {
        try {
            jenkinsClient.triggerJob("devsecops-pipeline", execution.getCommitHash());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean executeSecurityStage(PipelineExecution execution) {
        // FIX: SecurityScanRequest from correct package (request, not response)
        SecurityScanRequest request = new SecurityScanRequest();
        request.setProjectId(execution.getPipeline().getProject().getId());
        request.setExecutionId(execution.getId());

        // FIX: SecurityScanResponse is the correct return type (not the request class)
        SecurityScanResponse response = securityClient.scan(request);

        if (response == null) return false;

        // Check policy gate first (CRITICAL vuln = block regardless of score)
        if (Boolean.TRUE.equals(response.getBlocked())) return false;

        return response.getSecurityScore() != null
                && response.getSecurityScore() >= SECURITY_THRESHOLD;
    }

    @Override
    public List<ExecutionResponse> getExecutionsByPipeline(Long pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found"));

        return executionRepository.findByPipeline(pipeline).stream()
                .map(exec -> {
                    // FIX: load stages for each execution — was always null before
                    List<StageExecution> stages =
                            stageExecutionRepository.findByPipelineExecution(exec);
                    return executionMapper.toResponse(exec, stages);
                })
                .toList();
    }

    @Override
    public ExecutionResponse getExecutionById(Long executionId) {
        PipelineExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        // FIX: load stages — was always null before
        List<StageExecution> stages =
                stageExecutionRepository.findByPipelineExecution(execution);

        return executionMapper.toResponse(execution, stages);
    }
}

