package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.PipelineExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PipelineExecutionRepository extends JpaRepository<PipelineExecution, Long> {



    List<PipelineExecution> findByPipeline(Pipeline pipeline);
}