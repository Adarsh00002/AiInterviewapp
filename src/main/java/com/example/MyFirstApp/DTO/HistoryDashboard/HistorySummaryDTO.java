package com.example.MyFirstApp.DTO.HistoryDashboard;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HistorySummaryDTO {

    private Double score;

    private Double change;

    private Long interviews;

    private Integer streak;

    private String practiceTime;
}