package com.example.MyFirstApp.repository.QuickDrillSessionRepository;

import com.example.MyFirstApp.Entity.QuickDrillSession.QuickDrillQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuickDrillQuestionRepository
        extends JpaRepository<QuickDrillQuestion, Long> {

    List<QuickDrillQuestion> findByDrillIdOrderByQuestionNumberAsc(
            Long drillId
    );

    QuickDrillQuestion findByDrillIdAndQuestionNumber(
            Long drillId,
            Integer questionNumber
    );
}

