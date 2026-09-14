package com.example.MyFirstApp.DTO.QuickDrillDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickDrillStartResponse {

    private Long drillId;

    private Long questionId;

    private Integer currentQuestionNumber;

    private Integer totalQuestions;

    private String topic;

    private String difficulty;

    private String question;

    private Boolean completed;
}

