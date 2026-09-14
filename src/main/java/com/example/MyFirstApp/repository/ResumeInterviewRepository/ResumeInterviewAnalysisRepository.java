package com.example.MyFirstApp.repository.ResumeInterviewRepository;

import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeInterviewAnalysisRepository
        extends JpaRepository<ResumeInterviewAnalysis, Long> {

    List<ResumeInterviewAnalysis>
    findByUserIdOrderByCreatedAtDesc(Long userId);
}