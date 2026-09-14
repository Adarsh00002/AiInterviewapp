package com.example.MyFirstApp.DTO.ResumeInterviewDTO;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeInterviewStartResponse {

    private Long sessionId;
    private Long analysisId;
    private Integer currentQuestionNumber;
    /* * Interview duration in minutes. */
    private Integer sessionLength;
    /* * Remaining time in seconds when session starts. */
    private Long remainingSeconds;
    private String firstQuestion;
    private Boolean interviewCompleted;
}