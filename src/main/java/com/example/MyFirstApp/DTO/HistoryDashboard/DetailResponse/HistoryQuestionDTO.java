package com.example.MyFirstApp.DTO.HistoryDashboard.DetailResponse;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryQuestionDTO {

    private Integer questionNumber;

    private String question;

    private String userAnswer;

    private Double score;

    private String feedback;

    private String skill;

    private String difficulty;

    private String idealAnswer;

    private Boolean evaluated;
}