package com.example.MyFirstApp.DTO.AiCoach;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EndAiCoachResponse {

    private Long sessionId;

    private String status;

    private String message;
}