package com.example.MyFirstApp.Entity.HRandCoummunicationEntity;


import com.example.MyFirstApp.Enum.MessageSender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "conversation_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // SESSION
    // =========================================================

    @Column(nullable = false)
    private Long sessionId;

    // =========================================================
    // SENDER
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageSender sender;

    // =========================================================
    // MESSAGE
    // =========================================================

    @Column(columnDefinition = "TEXT")
    private String message;

    // =========================================================
    // OVERALL SCORE
    // =========================================================

    private Double score;

    // =========================================================
    // DIMENSION SCORES
    //
    // All stored on 0-100 scale
    // =========================================================

    private Double technicalScore;

    private Double communicationScore;

    private Double problemSolvingScore;

    private Double confidenceScore;

    // =========================================================
    // FEEDBACK
    // =========================================================

    @Column(columnDefinition = "TEXT")
    private String feedback;

    // =========================================================
    // TIME
    // =========================================================

    private LocalDateTime createdAt;
}