package com.example.MyFirstApp.DTO.QuickDrillDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickDrillSubmitRequest {

    private Long drillId;

    private Long questionId;

    private String answer;
}

