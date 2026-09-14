package com.example.MyFirstApp.DTO.VoiceInterviewDTO;

import lombok.Data;

@Data
public class LiveInterviewMessage {

    private String type;

    private Long sessionId;

    private String text;

    private String audioUrl;

    private Integer questionNumber;

    private Boolean completed;
}
