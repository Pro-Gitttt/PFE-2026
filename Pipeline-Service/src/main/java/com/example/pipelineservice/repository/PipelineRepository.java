package com.example.pipelineservice.repository;



import com.example.pipelineservice.entities.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineRepository extends JpaRepository<Pipeline, Long> {

}