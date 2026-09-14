package com.example.MyFirstApp.repository.ResumeInterviewRepository;

import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;

public interface ResumeInterviewQuestionRepository
        extends JpaRepository<ResumeInterviewQuestion, Long> {

    List<ResumeInterviewQuestion>
    findBySessionIdOrderByQuestionNumberAsc(
            Long sessionId
    );

    Optional<ResumeInterviewQuestion>
    findFirstBySessionIdAndQuestionNumber(
            Long sessionId,
            Integer questionNumber
    );
}