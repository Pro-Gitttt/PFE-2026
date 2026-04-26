package com.example.pipelineservice.client;

import com.example.pipelineservice.entities.PipelineExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class JenkinsClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String JENKINS_URL = "http://192.168.40.128:8080";
    private final String USER = "admin";
    private final String TOKEN = "1191735d78e67c11b3cbe11fba44d16f5f";

    // ---------------- CRUMB ----------------
    private String getCrumb() {

        String url = JENKINS_URL + "/crumbIssuer/api/json";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(USER, TOKEN);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET,
                        new HttpEntity<>(headers), Map.class);

        Map body = response.getBody();

        if (body == null || body.get("crumb") == null) {
            throw new RuntimeException("Cannot fetch Jenkins crumb");
        }

        return body.get("crumb").toString();
    }

    // ---------------- TRIGGER JOB ----------------
    public String triggerJob(String jobName, PipelineExecution execution) {

        try {
            String crumb = getCrumb();

            // IMPORTANT FIX: correct endpoint
            String url = JENKINS_URL + "/job/" + jobName + "/buildWithParameters";

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(USER, TOKEN);
            headers.add("Jenkins-Crumb", crumb);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("EXECUTION_ID", execution.getId().toString());

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            // 🔥 IMPORTANT FIX: handle missing location safely
            if (response.getHeaders().getLocation() == null) {
                throw new RuntimeException(
                        "Jenkins did not return queue URL. Check job config."
                );
            }

            String queueUrl = response.getHeaders().getLocation().toString();

            log.info("📦 Queue URL: {}", queueUrl);

            return queueUrl;

        } catch (Exception e) {
            log.error("❌ Jenkins trigger failed", e);
            throw new RuntimeException("Failed to trigger Jenkins job: " + e.getMessage());
        }
    }

    // ---------------- QUEUE ID ----------------
    public String extractQueueId(String queueUrl) {
        return queueUrl.replaceAll(".*/queue/item/(\\d+).*", "$1");
    }

    // ---------------- BUILD NUMBER ----------------
    public Integer getBuildNumber(String queueId) {

        try {
            String url = JENKINS_URL +
                    "/queue/item/" + queueId + "/api/json?tree=executable[number],cancelled";

            ResponseEntity<Map> response =
                    restTemplate.getForEntity(url, Map.class);

            Map body = response.getBody();

            if (body == null) return null;

            // if job was cancelled
            if (Boolean.TRUE.equals(body.get("cancelled"))) {
                throw new RuntimeException("Jenkins job was cancelled");
            }

            Map executable = (Map) body.get("executable");

            if (executable != null && executable.get("number") != null) {
                return (Integer) executable.get("number");
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }
}