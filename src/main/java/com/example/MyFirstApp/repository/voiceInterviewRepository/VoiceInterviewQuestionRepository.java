package com.example.MyFirstApp.repository.voiceInterviewRepository;

import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoiceInterviewQuestionRepository
        extends JpaRepository<VoiceInterviewQuestion, Long> {

    List<VoiceInterviewQuestion> findBySessionIdOrderByQuestionNumberAsc(
            Long sessionId
    );

    List<VoiceInterviewQuestion> findBySessionId(Long sessionId);

    long countBySessionId(Long sessionId);
}