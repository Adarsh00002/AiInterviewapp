package com.example.MyFirstApp.DTO.HistoryDashboard.DetailResponse;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryDetailResponse {

    private Long id;

    private String title;

    private String type;

    private Double score;

    private String tag;

    private LocalDateTime completedAt;

    private Integer durationSeconds;

    private Integer totalQuestions;

    private Integer answeredQuestions;

    private List<HistoryQuestionDTO> questions;

    private List<HistorySkillDTO> skills;

    private List<String> strengths;

    private List<String> improvements;

    private String finalFeedback;
}