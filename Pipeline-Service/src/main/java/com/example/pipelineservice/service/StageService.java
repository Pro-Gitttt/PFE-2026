package com.example.pipelineservice.service;



import com.example.pipelineservice.client.dto.request.CreateStageRequest;
import com.example.pipelineservice.client.dto.response.StageResponse;

import java.util.List;

public interface StageService {

    StageResponse createStage(Long pipelineId, CreateStageRequest request);

    List<StageResponse> getStagesByPipeline(Long pipelineId);

    StageResponse getStageById(Long stageId);

    void deleteStage(Long stageId);

}