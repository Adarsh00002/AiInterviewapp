package com.example.MyFirstApp.DTO.HistoryDashboard;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StreakDTO {

    private Integer days;

    private List<Boolean> week;
}