// ---------- WebhookService.java ----------
package com.example.pipelineservice.service;

import com.example.pipelineservice.client.dto.request.WebhookPayload;

public interface WebhookService {
    void handlePushEvent(WebhookPayload payload);
}