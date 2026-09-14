package com.example.MyFirstApp.Entity.HRandCoummunicationEntity;

import com.example.MyFirstApp.Enum.InterviewMode;
import com.example.MyFirstApp.Enum.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // USER
    // =========================================================

    @Column(nullable = false)
    private Long userId;

    // =========================================================
    // MODE
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewMode mode;

    // =========================================================
    // STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    // =========================================================
    // TIMING
    // =========================================================

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    // =========================================================
    // MESSAGE COUNT
    // =========================================================

    private Integer totalMessages;

    // =========================================================
    // FINAL SCORES
    // =========================================================

    private Double overallScore;

    private Double technicalScore;

    private Double communicationScore;

    private Double problemSolvingScore;

    private Double confidenceScore;

    // =========================================================
    // FINAL FEEDBACK
    // =========================================================

    @Column(columnDefinition = "TEXT")
    private String finalFeedback;
}