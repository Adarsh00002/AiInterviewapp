package com.example.MyFirstApp.DTO.HRandCommunication;


import lombok.Data;

@Data
public class UserMessageRequest {

    private Long sessionId;

    private String userMessage;
}