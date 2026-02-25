package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageRepository extends JpaRepository<Stage, Long> {}