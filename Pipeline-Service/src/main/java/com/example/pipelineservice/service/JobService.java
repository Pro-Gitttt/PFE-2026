package com.example.pipelineservice.service;



import com.example.pipelineservice.client.dto.request.CreateJobRequest;
import com.example.pipelineservice.client.dto.response.JobResponse;

import java.util.List;

public interface JobService {

    JobResponse createJob(Long stageId, CreateJobRequest request);

    List<JobResponse> getJobsByStage(Long stageId);

    JobResponse getJobById(Long jobId);

    void deleteJob(Long jobId);
}