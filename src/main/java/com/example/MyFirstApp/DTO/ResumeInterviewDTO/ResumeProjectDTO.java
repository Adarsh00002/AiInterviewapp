package com.example.MyFirstApp.DTO.ResumeInterviewDTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeProjectDTO {

    private String title;

    private String description;

    private List<String> technologies;

    private String projectImpact;
}