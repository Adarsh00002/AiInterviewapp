package com.example.MyFirstApp.DTO.ResumeInterviewDTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnswerRequest {

    private Long sessionId;

    private String answer;
}