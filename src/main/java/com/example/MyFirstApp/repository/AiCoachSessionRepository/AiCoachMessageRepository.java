package com.example.MyFirstApp.repository.AiCoachSessionRepository;

import com.example.MyFirstApp.Entity.AiCoah.AiCoachMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiCoachMessageRepository
        extends JpaRepository<AiCoachMessage, Long> {

    List<AiCoachMessage>
    findBySessionIdOrderByCreatedAtAsc(
            Long sessionId
    );

    @Modifying
    @Query(
            "DELETE FROM AiCoachMessage m WHERE m.sessionId = :sessionId"
    )
    int deleteBySessionId(
            @Param("sessionId") Long sessionId
    );
}