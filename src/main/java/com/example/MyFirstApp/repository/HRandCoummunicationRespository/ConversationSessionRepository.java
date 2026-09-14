package com.example.MyFirstApp.repository.HRandCoummunicationRespository;

import com.example.MyFirstApp.Entity.HRandCoummunicationEntity.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationSessionRepository
        extends JpaRepository<ConversationSession, Long> {
    List<ConversationSession>
    findByUserIdOrderByStartedAtDesc(
            Long userId
    );
}