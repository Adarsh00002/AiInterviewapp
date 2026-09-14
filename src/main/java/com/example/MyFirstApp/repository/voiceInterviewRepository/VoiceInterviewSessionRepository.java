package com.example.MyFirstApp.repository.voiceInterviewRepository;

import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoiceInterviewSessionRepository
        extends JpaRepository<VoiceInterviewSession, Long> {
    Optional<VoiceInterviewSession> findBySessionId(Long sessionId);
    List<VoiceInterviewSession>
    findByUserIdOrderByStartedAtDesc(
            Long userId
    );
}