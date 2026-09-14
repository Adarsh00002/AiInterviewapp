package com.example.MyFirstApp.DTO.QuickDrillDTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickDrillStartRequest {

    private List<String> topics;

    private String difficulty;

    private Integer questionCount;
}

