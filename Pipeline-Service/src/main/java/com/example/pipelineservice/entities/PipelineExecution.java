package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Tracks one run of a Pipeline triggered via Jenkins.
 *
 * FIXES:
 * - Added jenkinsQueueId  → returned by Jenkins POST /buildWithParameters
 * - Added jenkinsBuildNumber → resolved after job leaves Jenkins queue
 * - Added jenkinsBuildUrl → direct link to Jenkins console
 * - Removed @Data (Lombok conflict)
 * - Default status = PENDING (not null)
 * - @PrePersist for timestamps
 */
@Entity
@Table(name = "pipeline_execution")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PipelineExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 0 = triggered by webhook/system */
    private Long triggeredBy;

    private String commitHash;

    /** Jenkins queue item ID — from Location header after trigger */
    private Long jenkinsQueueId;

    /** Jenkins build number — resolved once job leaves queue */
    private Integer jenkinsBuildNumber;

    /** Deep link to Jenkins console output */
    private String jenkinsBuildUrl;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PipelineStatus status = PipelineStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    @PrePersist
    public void prePersist() {
        if (startTime == null) startTime = LocalDateTime.now();
        if (status    == null) status    = PipelineStatus.PENDING;
    }
}