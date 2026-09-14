package com.example.MyFirstApp.DTO.QuickDrillDTO;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickDrillResultResponse {

    private Long drillId;

    private Integer totalQuestions;

    private Integer answeredQuestions;

    private Double averageScore;

    private Double totalScore;

    private Boolean completed;

    private List<QuickDrillFeedbackResponse> results;
}