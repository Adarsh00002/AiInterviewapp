package com.example.MyFirstApp.DTO.NotificationDTO;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;

    private String type;

    private String title;

    private String message;

    private String entityType;

    private Long entityId;

    private Boolean isRead;

    private LocalDateTime createdAt;
}