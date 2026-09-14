package com.example.MyFirstApp.DTO.HistoryDashboard;


import com.example.MyFirstApp.DTO.HistoryDashboard.DetailResponse.InterviewTypePerformanceDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class HistoryDashboardResponse {

    private HistorySummaryDTO summary;

    private List<PerformancePointDTO> performance;

    private List<HistoryInterviewDTO> recentInterviews;

   private Map<String, List<SkillPerformanceDTO>> strengths;
    private Map<String, List<SkillPerformanceDTO>> improvements;

    private StreakDTO streak;

    private AIInsightDTO insight;

    private Map<String, InterviewTypePerformanceDTO> interviewTypePerformance;

}