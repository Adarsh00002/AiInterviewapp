package com.example.MyFirstApp.Entity.ResumeInterviewEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "resume_interview_question")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeInterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // SESSION
    // =========================================================

    @Column(nullable = false)
    private Long sessionId;

    // =========================================================
    // QUESTION
    // =========================================================

    @Column(nullable = false)
    private Integer questionNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    // =========================================================
    // CANDIDATE ANSWER
    // =========================================================

    @Column(columnDefinition = "TEXT")
    private String answer;

    // =========================================================
    // SCORES
    // =========================================================

    private Double score;

    private Double technicalScore;

    private Double communicationScore;

    private Double problemSolvingScore;

    private Double confidenceScore;

    // =========================================================
    // SKILL
    // =========================================================

    private String skill;

    // =========================================================
    // FEEDBACK
    // =========================================================

    @Column(columnDefinition = "TEXT")
    private String feedback;

    // =========================================================
    // TIMING
    // =========================================================

    private LocalDateTime createdAt;

    private LocalDateTime answeredAt;
}