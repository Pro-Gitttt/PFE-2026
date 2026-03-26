// ---------- CreateProjectRequest.java ----------
package com.example.pipelineservice.client.dto.request;

import com.example.pipelineservice.entities.VcsType;
import lombok.Data;

@Data
public class CreateProjectRequest {
    private String name;
    private String repositoryUrl;
    private String branch;
    private String owner;
    private VcsType vcsType;
}
 