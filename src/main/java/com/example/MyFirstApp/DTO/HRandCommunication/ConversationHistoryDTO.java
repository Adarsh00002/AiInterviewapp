package com.example.MyFirstApp.DTO.HRandCommunication;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationHistoryDTO {

    private String sender;

    private String message;
}