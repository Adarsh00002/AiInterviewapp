package com.example.MyFirstApp.DTO.AiCoach;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartAiCoachResponse {

    private Long sessionId;

    private String title;

    private String coachMode;

    private String aiMessage;

    private String status;
}