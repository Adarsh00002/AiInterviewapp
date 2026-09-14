package com.example.MyFirstApp.DTO.AiCoach;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiCoachMessageResponse {

    private Long sessionId;

    private String userMessage;

    private String aiMessage;

    private Boolean sessionActive;
}
