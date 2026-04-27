package com.example.securityservice.service;

import com.example.securityservice.entities.Vulnerability;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrivyParser {

    public List<Vulnerability> parse(MultipartFile file) {
        // TODO: read JSON + extract vulnerabilities
        return new ArrayList<>();
    }
}