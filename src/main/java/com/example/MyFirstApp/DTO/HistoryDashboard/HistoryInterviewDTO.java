package com.example.MyFirstApp.DTO.HistoryDashboard;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistoryInterviewDTO {

    private Long id;

    private String title;

    private String type;

    private Double score;

    private LocalDateTime completedAt;

    private Integer durationSeconds;

    private String tag;
}