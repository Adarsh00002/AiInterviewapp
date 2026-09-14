package com.example.MyFirstApp.repository.AiCoachSessionRepository;

import com.example.MyFirstApp.Entity.AiCoah.AiCoachSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiCoachSessionRepository
        extends JpaRepository<AiCoachSession, Long> {


    List<AiCoachSession>
    findByUserIdOrderByUpdatedAtDesc(
            Long userId
    );


    List<AiCoachSession>
    findByUserIdAndStatusOrderByUpdatedAtDesc(
            Long userId,
            com.example.MyFirstApp.Enum.AiCoachSessionStatus status
    );
}