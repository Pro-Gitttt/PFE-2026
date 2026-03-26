package com.example.pipelineservice.client.dto.request;

import lombok.Data;

@Data
public class ExecutionRequest {

    private Long userId;
    private String commitHash;
}