package com.example.MyFirstApp.Entity.voiceInterviewEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceInterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sessionId;

    private Integer questionNumber;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private Integer score;

    private String skill;

    @Column(name = "question_audio_url")
    private String questionAudioUrl;

    private Integer communicationScore;

    private Integer technicalKnowledgeScore;

    private Integer problemSolvingScore;

    private Integer confidenceClarityScore;

    @Column(columnDefinition = "TEXT")
    private String feedback;


    private LocalDateTime createdAt;
}