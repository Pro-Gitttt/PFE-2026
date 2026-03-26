package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.JobExecution;
import com.example.pipelineservice.entities.StageExecution;

import java.util.List;

public interface JobExecutionRepository {
    List<JobExecution> findByStageExecution(StageExecution stageExecution);
}

