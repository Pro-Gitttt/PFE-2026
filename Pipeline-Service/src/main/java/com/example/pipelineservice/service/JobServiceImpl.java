package com.example.pipelineservice.service;


import com.example.pipelineservice.MAPPER.JobMapper;
import com.example.pipelineservice.client.dto.request.CreateJobRequest;
import com.example.pipelineservice.client.dto.response.JobResponse;
import com.example.pipelineservice.entities.Job;
import com.example.pipelineservice.entities.Stage;

import com.example.pipelineservice.repository.JobRepository;
import com.example.pipelineservice.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final StageRepository stageRepository;
    private final JobMapper jobMapper;

    @Override
    public JobResponse createJob(Long stageId, CreateJobRequest request) {

        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found"));

        Job job = jobMapper.toEntity(request, stage);
        job.setCreatedAt(LocalDateTime.now());

        return jobMapper.toResponse(jobRepository.save(job));
    }

    @Override
    public List<JobResponse> getJobsByStage(Long stageId) {

        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found"));

        return jobRepository.findByStageOrderByIdAsc(stage)
                .stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    @Override
    public JobResponse getJobById(Long jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        return jobMapper.toResponse(job);
    }

    @Override
    public void deleteJob(Long jobId) {

        if (!jobRepository.existsById(jobId))
            throw new RuntimeException("Job not found");

        jobRepository.deleteById(jobId);
    }
}