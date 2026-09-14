package com.example.MyFirstApp.Entity.QuickDrillSession;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quick_drill_question")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickDrillQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Column(nullable = false)
    private Long drillId;

    @Column(nullable = false)
    private Integer questionNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String topic;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String userAnswer;

    private Double score;

    @Column(columnDefinition = "TEXT")
    private String whatYouDidWellJson;

    @Column(columnDefinition = "TEXT")
    private String whatToImproveJson;

    @Column(columnDefinition = "TEXT")
    private String idealAnswer;

    @Column(nullable = false)
    private Boolean evaluated;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime answeredAt;
}

