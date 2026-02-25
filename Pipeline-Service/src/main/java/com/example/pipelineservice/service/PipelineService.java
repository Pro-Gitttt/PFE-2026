package com.example.pipelineservice.service;

import com.example.pipelineservice.entities.*;

import java.util.List;

public interface PipelineService {

    Pipeline createPipeline(Pipeline pipeline);

    List<Pipeline> getAllPipelines();

    Pipeline getPipelineById(Long id);

    void deletePipeline(Long id);

    PipelineExecution triggerExecution(Long pipelineId, Long userId, String commitHash);

    List<PipelineExecution> getExecutionsByPipeline(Long pipelineId);

    Stage addStage(Long pipelineId, Stage stage);

    Job addJob(Long stageId, Job job);
}