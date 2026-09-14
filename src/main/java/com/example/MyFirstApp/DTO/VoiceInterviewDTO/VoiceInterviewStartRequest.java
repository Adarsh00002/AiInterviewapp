package com.example.MyFirstApp.DTO.VoiceInterviewDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceInterviewStartRequest {

    private Long userId;

    private String totalExperience;



    private String selectedRole;

    private String practiceType;

    private Integer sessionLength;

    private String interviewStyle;
}