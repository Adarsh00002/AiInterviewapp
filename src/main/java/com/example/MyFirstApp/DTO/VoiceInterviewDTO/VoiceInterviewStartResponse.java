package com.example.MyFirstApp.DTO.VoiceInterviewDTO;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VoiceInterviewStartResponse {

    private Long sessionId;

    private String audioUrl;
    private String aiSpeech;
}
