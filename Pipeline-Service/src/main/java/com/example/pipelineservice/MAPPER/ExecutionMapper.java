package com.example.pipelineservice.MAPPER;

import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.client.dto.response.StageExecutionResponse;
import com.example.pipelineservice.entities.PipelineExecution;
import com.example.pipelineservice.entities.StageExecution;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExecutionMapper {

    public ExecutionResponse toResponse(PipelineExecution execution,
                                        List<StageExecution> stageExecutions) {

        List<StageExecutionResponse> stages =
                stageExecutions.stream()
                        .map(this::mapStage)
                        .toList();

        return ExecutionResponse.builder()
                .id(execution.getId())
                .pipelineId(execution.getPipeline().getId())
                .status(execution.getStatus())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .stages(stages)
                .build();
    }

    private StageExecutionResponse mapStage(StageExecution stageExec) {

        return StageExecutionResponse.builder()
                .stageId(stageExec.getStage().getId())
                .stageName(stageExec.getStage().getName())
                .stageType(stageExec.getStage().getType())
                .status(stageExec.getStatus())
                .startTime(stageExec.getStartTime())
                .endTime(stageExec.getEndTime())
                .build();
    }
}