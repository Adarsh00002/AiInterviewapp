
        package com.example.MyFirstApp.DTO.QuickDrillDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickDrillFeedbackResponse {

    private Long drillId;

    private Long questionId;

    private Integer questionNumber;

    private Integer totalQuestions;

    private String topic;

    private String difficulty;

    private String question;

    private String userAnswer;

    private Double score;

    private List<String> whatYouDidWell;

    private List<String> whatToImprove;

    private String idealAnswer;

    private Boolean lastQuestion;

    private Boolean evaluated;
}

