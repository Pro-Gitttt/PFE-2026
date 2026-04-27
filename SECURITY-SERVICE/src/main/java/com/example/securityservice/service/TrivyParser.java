package com.example.securityservice.service;

import com.example.securityservice.entities.ScanType;
import com.example.securityservice.entities.SeverityLevel;
import com.example.securityservice.entities.Vulnerability;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TrivyParser {

    public List<Vulnerability> parse(MultipartFile file) {

        List<Vulnerability> list = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map json = mapper.readValue(file.getInputStream(), Map.class);

            List<Map> results = (List<Map>) json.get("Results");

            if (results == null) return list;

            for (Map result : results) {
                List<Map> vulns = (List<Map>) result.get("Vulnerabilities");

                if (vulns == null) continue;

                for (Map v : vulns) {
                    list.add(
                            Vulnerability.builder()
                                    .type(ScanType.SCA)
                                    .severity(mapSeverity((String) v.get("Severity")))
                                    .description((String) v.get("Title"))
                                    .cve((String) v.get("VulnerabilityID"))
                                    .build()
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private SeverityLevel mapSeverity(String s) {
        return switch (s) {
            case "CRITICAL" -> SeverityLevel.CRITICAL;
            case "HIGH" -> SeverityLevel.HIGH;
            case "MEDIUM" -> SeverityLevel.MEDIUM;
            default -> SeverityLevel.LOW;
        };
    }
}