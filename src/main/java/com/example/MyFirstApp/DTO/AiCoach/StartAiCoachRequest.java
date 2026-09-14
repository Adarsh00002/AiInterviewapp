package com.example.MyFirstApp.DTO.AiCoach;

import lombok.Data;

@Data
public class StartAiCoachRequest {

    private Long userId;

    /*
     * GENERAL
     * INTERVIEW
     * COMMUNICATION
     * CAREER
     * RESUME
     */

    private String coachMode;
}