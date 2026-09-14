package com.example.MyFirstApp.DTO.HRandCommunication;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartConversationResponse {

    private Long sessionId;

    private String aiMessage;

    private String audioUrl;

    private String status;
}