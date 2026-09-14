package com.example.MyFirstApp.DTO.AiCoach;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiCoachHistoryDTO {

    private Long id;

    private String sender;

    private String message;

    private LocalDateTime createdAt;
}