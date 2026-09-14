package com.example.MyFirstApp.DTO.HistoryDashboard;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillPerformanceDTO {

    private String name;

    private Double value;
}