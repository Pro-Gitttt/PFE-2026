package com.example.pipelineservice.controller;

import com.example.pipelineservice.client.dto.request.ExecutionRequest;
import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping("/{pipelineId}")
    public ExecutionResponse execute(
            @PathVariable Long pipelineId,
            @RequestBody ExecutionRequest request) {

        return executionService.executePipeline(pipelineId, request);
    }

    @GetMapping("/pipeline/{pipelineId}")
    public List<ExecutionResponse> getByPipeline(@PathVariable Long pipelineId) {
        return executionService.getExecutionsByPipeline(pipelineId);
    }

    @GetMapping("/{executionId}")
    public ExecutionResponse getById(@PathVariable Long executionId) {
        return executionService.getExecutionById(executionId);
    }
}