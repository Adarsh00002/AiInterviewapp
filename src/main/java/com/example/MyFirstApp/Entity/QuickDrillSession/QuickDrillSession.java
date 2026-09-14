package com.example.MyFirstApp.Entity.QuickDrillSession;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quick_drill_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickDrillSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long drillId;

    @Column(nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String topicsJson;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false)
    private Integer currentQuestionNumber;

    @Column(nullable = false)
    private Integer answeredQuestions;

    @Column(nullable = false)
    private Double totalScore;

    @Column(nullable = false)
    private Boolean completed;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}

