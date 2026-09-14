package com.example.MyFirstApp.DTO.VoiceInterviewDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResultDTO {
    private String name;
    private String skill;
    private Integer score;
}
