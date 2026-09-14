package com.example.MyFirstApp.DTO.HistoryDashboard.DetailResponse;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorySkillDTO {

    private String name;

    private Double value;
}