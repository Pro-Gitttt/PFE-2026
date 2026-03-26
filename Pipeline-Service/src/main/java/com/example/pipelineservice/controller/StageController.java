package com.example.pipelineservice.controller;


import com.example.pipelineservice.client.dto.request.CreateStageRequest;
import com.example.pipelineservice.client.dto.response.StageResponse;
import com.example.pipelineservice.service.StageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;

    @PostMapping("/pipeline/{pipelineId}")
    public StageResponse createStage(
            @PathVariable Long pipelineId,
            @RequestBody CreateStageRequest request) {

        return stageService.createStage(pipelineId, request);
    }

    @GetMapping("/pipeline/{pipelineId}")
    public List<StageResponse> getStagesByPipeline(
            @PathVariable Long pipelineId) {

        return stageService.getStagesByPipeline(pipelineId);
    }

    @GetMapping("/{stageId}")
    public StageResponse getStage(
            @PathVariable Long stageId) {

        return stageService.getStageById(stageId);
    }

    @DeleteMapping("/{stageId}")
    public void deleteStage(
            @PathVariable Long stageId) {

        stageService.deleteStage(stageId);
    }
}