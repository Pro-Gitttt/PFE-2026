package com.example.pipelineservice.controller;

import com.example.pipelineservice.client.dto.request.ExecutionRequest;
import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * Trigger a pipeline execution.
     * Always returns 202 ACCEPTED with status=PENDING.
     * Poll GET /api/executions/{id} to track progress.
     */
    @PostMapping("/{pipelineId}")
    public ResponseEntity<ExecutionResponse> execute(
            @PathVariable Long pipelineId,
            @RequestBody ExecutionRequest request) {

        ExecutionResponse response = executionService.executePipeline(pipelineId, request);

        URI pollingUri = URI.create("/api/executions/" + response.getId());

        return ResponseEntity
                .accepted()                 // 202 — tells the caller "in progress, poll me"
                .location(pollingUri)       // Location: /api/executions/95
                .body(response);
    }

    /**
     * Poll this to get live status: PENDING → RUNNING → SUCCESS / FAILED
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<ExecutionResponse> getById(@PathVariable Long executionId) {
        return ResponseEntity.ok(executionService.getExecutionById(executionId));
    }

    /**
     * Get all executions for a pipeline, newest first.
     */
    @GetMapping("/pipeline/{pipelineId}")
    public ResponseEntity<List<ExecutionResponse>> getByPipeline(@PathVariable Long pipelineId) {
        return ResponseEntity.ok(executionService.getExecutionsByPipeline(pipelineId));
    }
}