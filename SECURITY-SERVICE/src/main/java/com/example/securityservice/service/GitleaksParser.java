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
public class GitleaksParser {

    public List<Vulnerability> parse(MultipartFile file) {
        List<Vulnerability> list = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> leaks =
                    mapper.readValue(file.getInputStream(), List.class);

            for (Map<String, Object> leak : leaks) {
                list.add(
                        Vulnerability.builder()
                                .type(ScanType.SECRET)
                                .severity(SeverityLevel.CRITICAL)
                                .description((String) leak.get("Description"))
                                .filePath((String) leak.get("File"))
                                .build()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}