package com.example.MyFirstApp.DTO.HRandCommunication;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EndInterviewResponse {

    private Double overallScore;

    private String finalFeedback;

    private Integer totalMessages;
}