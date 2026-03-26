// ---------- ExecutionResponse.java ----------
package com.example.pipelineservice.client.dto.response;

import com.example.pipelineservice.entities.PipelineStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExecutionResponse {
    private Long id;
    private Long pipelineId;
    private String commitHash;
    private PipelineStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    // FIX: was never populated in original code — now correctly filled via ExecutionMapper
    private List<StageExecutionResponse> stages;
}