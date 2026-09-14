package com.example.MyFirstApp.DTO.HRandCommunication;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiConversationResponse {

    private String aiMessage;

    private String audioUrl;

    private Double currentScore;

    private String feedback;

    private Boolean interviewCompleted;
}