package com.example.MyFirstApp.DTO.HistoryDashboard.DetailResponse;


import com.example.MyFirstApp.DTO.HistoryDashboard.SkillPerformanceDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewTypePerformanceDTO {

    private List<SkillPerformanceDTO> strengths;

    private List<SkillPerformanceDTO> improvements;
}