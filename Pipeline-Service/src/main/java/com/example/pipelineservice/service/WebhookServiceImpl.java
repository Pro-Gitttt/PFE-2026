// ---------- WebhookServiceImpl.java ----------
package com.example.pipelineservice.service;

import com.example.pipelineservice.client.dto.request.ExecutionRequest;
import com.example.pipelineservice.client.dto.request.WebhookPayload;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Project;
import com.example.pipelineservice.exception.ResourceNotFoundException;
import com.example.pipelineservice.repository.PipelineRepository;
import com.example.pipelineservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final ProjectRepository  projectRepository;
    private final PipelineRepository pipelineRepository;
    private final ExecutionService   executionService;

    @Override
    public void handlePushEvent(WebhookPayload payload) {

        Project project = projectRepository
                .findByRepositoryUrl(payload.getRepositoryUrl())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No project found for repo: " + payload.getRepositoryUrl()));

        Pipeline pipeline = pipelineRepository
                .findFirstByProject(project)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pipeline found for project: " + project.getName()));

        ExecutionRequest request = new ExecutionRequest();
        request.setCommitHash(payload.getCommitHash());
        request.setUserId(0L); // system-triggered

        executionService.executePipeline(pipeline.getId(), request);
    }
}
 