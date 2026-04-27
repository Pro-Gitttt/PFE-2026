package com.example.securityservice.service;

import com.example.securityservice.entities.ScanType;
import com.example.securityservice.entities.SeverityLevel;
import com.example.securityservice.entities.Vulnerability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SastScanner {

    private final RestTemplate restTemplate;

    private static final String SONAR_URL = "http://192.168.40.128:9000";
    private static final String TOKEN = "sqa_4028d3afe1c221d943755d9e5123c8b91f770d9b";

    public List<Vulnerability> scan(Long projectId) {

        // Example project key mapping
        String projectKey = "pipeline-service";

        String url = SONAR_URL +
                "/api/issues/search?projectKey=" + projectKey +
                "&types=VULNERABILITY,BUG";

        Map response = restTemplate.getForObject(
                url,
                Map.class
        );

        List<Map> issues = (List<Map>) response.get("issues");

        List<Vulnerability> vulnerabilities = new ArrayList<>();

        if (issues == null) return vulnerabilities;

        for (Map issue : issues) {

            String severity = (String) issue.get("severity");

            vulnerabilities.add(
                    Vulnerability.builder()
                            .type(ScanType.SAST)
                            .severity(mapSeverity(severity))
                            .description((String) issue.get("message"))
                            .filePath((String) issue.get("component"))
                            .build()
            );
        }

        return vulnerabilities;
    }

    private SeverityLevel mapSeverity(String sonarSeverity) {
        return switch (sonarSeverity) {
            case "BLOCKER", "CRITICAL" -> SeverityLevel.CRITICAL;
            case "MAJOR" -> SeverityLevel.HIGH;
            case "MINOR" -> SeverityLevel.MEDIUM;
            default -> SeverityLevel.LOW;
        };
    }
}