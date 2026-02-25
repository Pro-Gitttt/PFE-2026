package com.example.pipelineservice.client.dto;

public class SecurityScanResponse {

    private Long id;
    private Double securityScore;
    private Boolean blocked;

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public Long getId() {
        return id;
    }

    public Double getSecurityScore() {
        return securityScore;
    }
}