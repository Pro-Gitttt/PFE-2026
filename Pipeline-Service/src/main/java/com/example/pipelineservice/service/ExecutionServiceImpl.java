package com.example.pipelineservice.service;

import com.example.pipelineservice.MAPPER.ExecutionMapper;
import com.example.pipelineservice.client.JenkinsClient;
import com.example.pipelineservice.client.SecurityClient;
import com.example.pipelineservice.client.dto.request.ExecutionRequest;
import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.entities.*;
import com.example.pipelineservice.exception.ResourceNotFoundException;
import com.example.pipelineservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExecutionServiceImpl implements ExecutionService {

    private final PipelineRepository pipelineRepository;
    private final PipelineExecutionRepository executionRepository;
    private final StageRepository stageRepository;
    private final StageExecutionRepository stageExecutionRepository;
    private final ExecutionMapper executionMapper;
    private final JenkinsClient jenkinsClient;
    private final SecurityClient securityClient;

    @Override
    public ExecutionResponse executePipeline(Long pipelineId, ExecutionRequest request) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found"));

        PipelineExecution execution = PipelineExecution.builder()
                .pipeline(pipeline)
                .triggeredBy(request.getUserId())
                .commitHash(request.getCommitHash())
                .status(PipelineStatus.PENDING)
                .startTime(LocalDateTime.now())
                .build();

        execution = executionRepository.save(execution);

        // =========================
        // TRIGGER JENKINS
        // =========================
        String queueUrl = jenkinsClient.triggerJob(
                pipeline.getJenkinsJobName(),
                execution
        );

        String queueId = extractQueueId(queueUrl);

        execution.setJenkinsQueueId(Long.parseLong(queueId));
        executionRepository.save(execution);

        final PipelineExecution execRef = execution;
        final String finalQueueId = queueId;

        // =========================
        // ASYNC TRACKING (FIXED LOOP)
        // =========================
        new Thread(() -> {

            try {
                Integer buildNumber = null;

                for (int i = 0; i < 60; i++) {

                    buildNumber = jenkinsClient.getBuildNumber(finalQueueId);

                    if (buildNumber != null) {

                        execRef.setJenkinsBuildNumber(buildNumber);

                        execRef.setJenkinsBuildUrl(
                                "http://192.168.40.128:8080/job/" +
                                        pipeline.getJenkinsJobName() +
                                        "/" + buildNumber
                        );

                        execRef.setStatus(PipelineStatus.RUNNING);

                        executionRepository.save(execRef);
                        return;
                    }

                    Thread.sleep(2000);
                }

                execRef.setStatus(PipelineStatus.FAILED);
                executionRepository.save(execRef);

            } catch (Exception e) {
                log.error("Jenkins tracking failed", e);
                execRef.setStatus(PipelineStatus.FAILED);
                executionRepository.save(execRef);
            }

        }).start();

        return executionMapper.toResponse(execution, List.of());
    }

    // =========================
    // HELPER
    // =========================
    private String extractQueueId(String queueUrl) {
        return queueUrl.replaceAll(".*/queue/item/(\\d+).*", "$1");
    }

    // =========================
    // GET ALL EXECUTIONS
    // =========================
    @Override
    public List<ExecutionResponse> getExecutionsByPipeline(Long pipelineId) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found"));

        return executionRepository.findByPipelineOrderByStartTimeDesc(pipeline)
                .stream()
                .map(exec -> executionMapper.toResponse(exec, List.of()))
                .toList();
    }

    // =========================
    // GET BY ID
    // =========================
    @Override
    public ExecutionResponse getExecutionById(Long executionId) {

        PipelineExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        return executionMapper.toResponse(execution, List.of());
    }

    // =========================
    // UPDATE STATUS
    // =========================
    @Override
    public void updateStatus(Long executionId, PipelineStatus status) {

        PipelineExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        execution.setStatus(status);
        executionRepository.save(execution);
    }
}