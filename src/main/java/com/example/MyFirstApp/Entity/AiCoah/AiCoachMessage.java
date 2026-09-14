package com.example.MyFirstApp.Entity.AiCoah;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_coach_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCoachMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // SESSION
    // =========================================================

    private Long sessionId;


    // =========================================================
    // SENDER
    //
    // USER / AI
    // =========================================================

    @Column(length = 20)
    private String sender;


    // =========================================================
    // MESSAGE
    // =========================================================

    @Column(columnDefinition = "TEXT")
    private String message;


    // =========================================================
    // CREATED
    // =========================================================

    private LocalDateTime createdAt;
}