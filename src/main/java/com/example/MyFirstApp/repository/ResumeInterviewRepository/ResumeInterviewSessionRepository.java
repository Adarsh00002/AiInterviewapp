package com.example.MyFirstApp.repository.ResumeInterviewRepository;



import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeInterviewSessionRepository
        extends JpaRepository<ResumeInterviewSession, Long> {

    List<ResumeInterviewSession>
    findByUserIdOrderByStartedAtDesc(
            Long userId
    );
}