package com.example.MyFirstApp.DTO.VoiceInterviewDTO;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceInterviewResultResponse {

    private Long sessionId;



    private String selectedRole;

    // Overall
    private Integer overallScore;

    private String performance;

    private Integer totalQuestions;

    private Integer answeredQuestions;

    private Integer totalScore;

    private String questionAudioUrl;

    private Double averageScore;

    // Time
    private Long timeTakenSeconds;

    private Long totalTimeSeconds;

    private Double communicationScore;
    private Double technicalKnowledgeScore;
    private Double problemSolvingScore;
    private Double confidenceClarityScore;

    // Skills
    private List<SkillResultDTO> skills;

    // Strengths
    private List<String> strengths;

    // Improvements
    private List<String> improvements;

    // Question wise review
    private List<QuestionReviewDTO> reviews;

}