package com.example.MyFirstApp.DTO.VoiceInterviewDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionReviewDTO {
    private Integer questionNumber;

    private String question;

    private String questionAudioUrl;

    private Integer score;

    private String feedback;

    private String performance;
    private String skill;
}
