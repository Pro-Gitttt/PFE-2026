package com.example.pipelineservice.controller;

import com.example.pipelineservice.client.dto.request.WebhookPayload;
import com.example.pipelineservice.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/push")
    public String handlePush(@RequestBody WebhookPayload payload) {

        webhookService.handlePushEvent(payload);

        return "Pipeline triggered";
    }
}