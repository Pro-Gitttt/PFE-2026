package com.example.pipelineservice.client.dto.response;

import com.example.pipelineservice.entities.VcsType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectResponse {

    private Long id;
    private String name;
    private String repositoryUrl;
    private String branch;
    private VcsType vcsType;
    private LocalDateTime createdAt;
}