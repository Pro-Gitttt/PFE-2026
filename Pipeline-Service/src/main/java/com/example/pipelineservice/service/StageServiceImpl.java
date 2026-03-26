package com.example.pipelineservice.service;


import com.example.pipelineservice.MAPPER.StageMapper;
import com.example.pipelineservice.client.dto.request.CreateStageRequest;
import com.example.pipelineservice.client.dto.response.StageResponse;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Stage;

import com.example.pipelineservice.repository.PipelineRepository;
import com.example.pipelineservice.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StageServiceImpl implements StageService {

    private final StageRepository stageRepository;
    private final PipelineRepository pipelineRepository;
    private final StageMapper stageMapper;

    @Override
    public StageResponse createStage(Long pipelineId, CreateStageRequest request) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new RuntimeException("Pipeline not found"));

        Stage stage = stageMapper.toEntity(request, pipeline);

        return stageMapper.toResponse(stageRepository.save(stage));
    }

    @Override
    public List<StageResponse> getStagesByPipeline(Long pipelineId) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new RuntimeException("Pipeline not found"));

        return stageRepository.findByPipelineOrderByOrderIndexAsc(pipeline)
                .stream()
                .map(stageMapper::toResponse)
                .toList();
    }

    @Override
    public StageResponse getStageById(Long stageId) {

        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found"));

        return stageMapper.toResponse(stage);
    }

    @Override
    public void deleteStage(Long stageId) {

        if (!stageRepository.existsById(stageId))
            throw new RuntimeException("Stage not found");

        stageRepository.deleteById(stageId);
    }
}