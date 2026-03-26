// ---------- JenkinsClient.java ----------
package com.example.pipelineservice.client;

import org.springframework.stereotype.Component;

@Component
public class JenkinsClient {

    public void triggerJob(String jobName, String commitHash) {
        // TODO: integrate real Jenkins REST API call here
        System.out.println("Triggering Jenkins job: " + jobName
                + " | commit: " + commitHash);
    }
}