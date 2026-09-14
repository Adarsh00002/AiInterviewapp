package com.example.MyFirstApp.DTO.ResumeInterviewDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisResponse {

    private Long analysisId;

    private String fileName;

    private Integer resumeScore;

    private String overallSummary;

    private String background;

    private List<String> skills;

    private List<ResumeProjectDTO> projects;

    private List<String> strengths;

    private List<String> improvements;

    private List<String> interviewFocus;
}