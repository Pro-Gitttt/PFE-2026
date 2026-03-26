// ---------- PipelineMapper.java ----------
package com.example.pipelineservice.MAPPER;

import com.example.pipelineservice.client.dto.request.CreatePipelineRequest;
import com.example.pipelineservice.client.dto.response.PipelineResponse;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Project;
import org.springframework.stereotype.Component;

@Component
public class PipelineMapper {

    public Pipeline toEntity(CreatePipelineRequest request, Project project) {
        return Pipeline.builder()
                .name(request.getName())
                .project(project)
                .build();
    }

    public PipelineResponse toResponse(Pipeline pipeline) {
        return PipelineResponse.builder()
                .id(pipeline.getId())
                .name(pipeline.getName())
                .projectId(pipeline.getProject().getId())
                // FIX: status and createdAt now included in response
                .status(pipeline.getStatus())
                .createdAt(pipeline.getCreatedAt())
                .build();
    }
}
 
