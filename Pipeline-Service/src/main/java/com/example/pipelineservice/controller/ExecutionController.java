package com.example.pipelineservice.controller;

import com.example.pipelineservice.client.dto.request.ExecutionRequest;
import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    // =========================================
    // EXECUTE PIPELINE
    // =========================================
    @PostMapping("/{pipelineId}/executions")
    public ResponseEntity<ExecutionResponse> executePipeline(
            @PathVariable Long pipelineId,
            @RequestBody ExecutionRequest request) {

        ExecutionResponse response =
                executionService.executePipeline(pipelineId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================
    // GET EXECUTIONS BY PIPELINE
    // =========================================
    @GetMapping("/{pipelineId}/executions")
    public ResponseEntity<List<ExecutionResponse>> getExecutionsByPipeline(
            @PathVariable Long pipelineId) {

        return ResponseEntity.ok(
                executionService.getExecutionsByPipeline(pipelineId)
        );
    }

    // =========================================
    // GET EXECUTION BY ID
    // =========================================
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ExecutionResponse> getExecution(
            @PathVariable Long executionId) {

        return ResponseEntity.ok(
                executionService.getExecutionById(executionId)
        );
    }
}