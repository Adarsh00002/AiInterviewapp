package com.example.MyFirstApp.Entity.voiceInterviewEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceInterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    // =========================================================
    // USER
    // =========================================================

    @Column(nullable = false)
    private Long userId;

    // =========================================================
    // EXPERIENCE
    // =========================================================

    private String experienceLevel;

    private String totalExperience;

    private String selectedRole;

    // =========================================================
    // AUDIO
    // =========================================================

    private String currentQuestionAudioUrl;

    // =========================================================
    // VOICE INTERVIEW SETUP
    // =========================================================

    private String practiceType;

    private Integer sessionLength;

    private String interviewStyle;

    // =========================================================
    // PROGRESS
    // =========================================================

    private Integer currentQuestionNumber;

    @Column(columnDefinition = "TEXT")
    private String currentQuestion;

    private Integer totalScore;

    private Boolean completed;

    // =========================================================
    // TIMING
    // =========================================================

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}