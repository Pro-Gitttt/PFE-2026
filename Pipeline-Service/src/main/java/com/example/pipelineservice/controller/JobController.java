package com.example.pipelineservice.controller;


import com.example.pipelineservice.client.dto.request.CreateJobRequest;
import com.example.pipelineservice.client.dto.response.JobResponse;
import com.example.pipelineservice.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping("/stage/{stageId}")
    public JobResponse createJob(
            @PathVariable Long stageId,
            @RequestBody CreateJobRequest request) {

        return jobService.createJob(stageId, request);
    }

    @GetMapping("/stage/{stageId}")
    public List<JobResponse> getJobsByStage(
            @PathVariable Long stageId) {

        return jobService.getJobsByStage(stageId);
    }

    @GetMapping("/{jobId}")
    public JobResponse getJob(
            @PathVariable Long jobId) {

        return jobService.getJobById(jobId);
    }

    @DeleteMapping("/{jobId}")
    public void deleteJob(
            @PathVariable Long jobId) {

        jobService.deleteJob(jobId);
    }
}