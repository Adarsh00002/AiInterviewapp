package com.example.MyFirstApp.Entity.ResumeInterviewEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_interview_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeInterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    private Long userId;

    private Long analysisId;

    private Integer currentQuestionNumber;

    private Integer totalQuestions;

    @Column(columnDefinition = "TEXT")
    private String currentQuestion;

    private String interviewStyle;

    private Integer sessionLength;

    private Boolean completed;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}