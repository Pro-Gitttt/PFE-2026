package com.example.pipelineservice.service;

import com.example.pipelineservice.client.SecurityClient;
import com.example.pipelineservice.client.dto.SecurityScanResponse;
import com.example.pipelineservice.entities.*;
import com.example.pipelineservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository pipelineRepository;
    private final PipelineExecutionRepository executionRepository;
    private final StageRepository stageRepository;
    private final JobRepository jobRepository;
    private final SecurityClient securityClient;

    // ================= PIPELINE =================

    @Override
    public Pipeline createPipeline(Pipeline pipeline) {
        pipeline.setStatus(PipelineStatus.CREATED);
        pipeline.setCreatedAt(LocalDateTime.now());
        return pipelineRepository.save(pipeline);
    }

    @Override
    public List<Pipeline> getAllPipelines() {
        return pipelineRepository.findAll();
    }

    @Override
    public Pipeline getPipelineById(Long id) {
        return pipelineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline not found"));
    }

    @Override
    public void deletePipeline(Long id) {
        pipelineRepository.deleteById(id);
    }

    // ================= EXECUTION =================

    @Override
    public PipelineExecution triggerExecution(Long pipelineId,
                                              Long userId,
                                              String commitHash) {

        Pipeline pipeline = getPipelineById(pipelineId);

        pipeline.setStatus(PipelineStatus.RUNNING);
        pipelineRepository.save(pipeline);

        // 🔹 Use RELATION not pipelineId
        PipelineExecution execution = new PipelineExecution();
        execution.setPipeline(pipeline);
        execution.setTriggeredBy(userId);
        execution.setCommitHash(commitHash);
        execution.setStartTime(LocalDateTime.now());
        execution.setStatus(PipelineStatus.RUNNING);

        execution = executionRepository.save(execution);

        // 🔥 CALL SECURITY SERVICE
        SecurityScanResponse scan =
                securityClient.performScan(execution.getId(), "SAST");

        // 🔐 POLICY ENFORCEMENT
        if (Boolean.TRUE.equals(scan.getBlocked())) {
            execution.setStatus(PipelineStatus.FAILED);
            pipeline.setStatus(PipelineStatus.FAILED);
        } else {
            execution.setStatus(PipelineStatus.SUCCESS);
            pipeline.setStatus(PipelineStatus.SUCCESS);
        }

        pipelineRepository.save(pipeline);

        return executionRepository.save(execution);
    }

    @Override
    public List<PipelineExecution> getExecutionsByPipeline(Long pipelineId) {

        Pipeline pipeline = getPipelineById(pipelineId);

        return executionRepository.findByPipeline(pipeline);
    }

    // ================= STAGE =================

    @Override
    public Stage addStage(Long pipelineId, Stage stage) {

        Pipeline pipeline = getPipelineById(pipelineId);

        stage.setPipeline(pipeline);   // 🔹 RELATION
        return stageRepository.save(stage);
    }

    // ================= JOB =================

    @Override
    public Job addJob(Long stageId, Job job) {

        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found"));

        job.setStage(stage);   // 🔹 RELATION
        job.setStatus(PipelineStatus.CREATED);

        return jobRepository.save(job);
    }
}