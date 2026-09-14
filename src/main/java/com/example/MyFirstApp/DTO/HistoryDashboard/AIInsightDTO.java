package com.example.MyFirstApp.DTO.HistoryDashboard;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIInsightDTO {

    private String title;

    private String message;
}