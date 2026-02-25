package com.example.pipelineservice.controller;

import com.example.pipelineservice.client.dto.SecurityScanResponse;
import com.example.pipelineservice.entities.*;
import com.example.pipelineservice.service.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;


    @PostMapping
    public Pipeline create(@RequestBody Pipeline pipeline) {
        return pipelineService.createPipeline(pipeline);
    }

    @GetMapping
    public List<Pipeline> getAll() {
        return pipelineService.getAllPipelines();
    }

    @GetMapping("/{id}")
    public Pipeline getById(@PathVariable Long id) {
        return pipelineService.getPipelineById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        pipelineService.deletePipeline(id);
    }


}