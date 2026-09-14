package com.example.MyFirstApp.DTO.HistoryDashboard;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PerformancePointDTO {

    private LocalDate date;

    private Double score;
}