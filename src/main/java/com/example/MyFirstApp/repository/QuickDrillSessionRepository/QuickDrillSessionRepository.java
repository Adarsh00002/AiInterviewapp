package com.example.MyFirstApp.repository.QuickDrillSessionRepository;

import com.example.MyFirstApp.Entity.QuickDrillSession.QuickDrillSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuickDrillSessionRepository
        extends JpaRepository<QuickDrillSession, Long> {

    List<QuickDrillSession> findByUserIdOrderByStartedAtDesc(
            Long userId
    );

}

