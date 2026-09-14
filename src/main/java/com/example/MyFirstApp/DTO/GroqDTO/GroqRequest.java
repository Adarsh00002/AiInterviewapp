package com.example.MyFirstApp.DTO.GroqDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Builder
public class GroqRequest {

    private String model;

    private List<Message> messages;

    private Boolean stream;

    public GroqRequest(
            String model,
            List<Message> messages) {

        this.model = model;
        this.messages = messages;
        this.stream = false;
    }

    public GroqRequest(
            String model,
            List<Message> messages,
            Boolean stream) {

        this.model = model;
        this.messages = messages;
        this.stream = stream;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {

        private String role;

        private String content;
    }
}