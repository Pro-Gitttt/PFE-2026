package com.example.pipelineservice.client.dto.response;

import com.example.pipelineservice.entities.PipelineStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PipelineResponse {
    private Long id;
    private String name;
    private Long projectId;
    // FIX: added status and createdAt — useful for dashboard display
    private PipelineStatus status;
    private LocalDateTime createdAt;
}