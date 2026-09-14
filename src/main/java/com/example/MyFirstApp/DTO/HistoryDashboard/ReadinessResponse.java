package com.example.MyFirstApp.DTO.HistoryDashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReadinessResponse {

    private Double overallScore;

    private Double technical;

    private Double communication;

    private Double problemSolving;

    private Double confidence;
}