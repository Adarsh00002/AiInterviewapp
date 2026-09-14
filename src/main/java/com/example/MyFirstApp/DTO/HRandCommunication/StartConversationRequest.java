package com.example.MyFirstApp.DTO.HRandCommunication;

import com.example.MyFirstApp.Enum.InterviewMode;
import lombok.Data;

@Data
public class StartConversationRequest {

    private Long userId;

    private InterviewMode mode;
}