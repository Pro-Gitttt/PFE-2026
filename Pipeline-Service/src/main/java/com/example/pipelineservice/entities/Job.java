package com.example.pipelineservice.entities;

import com.example.pipelineservice.entities.Stage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int orderIndex;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;
}