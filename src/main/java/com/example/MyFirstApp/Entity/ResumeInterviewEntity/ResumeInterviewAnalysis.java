package com.example.MyFirstApp.Entity.ResumeInterviewEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_interview_analysis")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeInterviewAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    private Integer resumeScore;

    @Column(columnDefinition = "TEXT")
    private String overallSummary;

    @Column(columnDefinition = "TEXT")
    private String background;

    @Column(columnDefinition = "TEXT")
    private String skillsJson;

    @Column(columnDefinition = "TEXT")
    private String projectsJson;

    @Column(columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(columnDefinition = "TEXT")
    private String improvementsJson;

    @Column(columnDefinition = "TEXT")
    private String interviewFocusJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}