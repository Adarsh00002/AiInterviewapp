package com.example.MyFirstApp.Entity.AiCoah;

import com.example.MyFirstApp.Enum.AiCoachSessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_coach_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCoachSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // USER
    // =========================================================

    private Long userId;


    // =========================================================
    // SESSION TITLE
    // =========================================================

    @Column(length = 200)
    private String title;


    // =========================================================
    // OPTIONAL COACH MODE
    //
    // GENERAL
    // INTERVIEW
    // COMMUNICATION
    // CAREER
    // RESUME
    // =========================================================

    @Column(length = 50)
    private String coachMode;


    // =========================================================
    // STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    private AiCoachSessionStatus status;


    // =========================================================
    // CREATED
    // =========================================================

    private LocalDateTime createdAt;


    // =========================================================
    // LAST UPDATED
    // =========================================================

    private LocalDateTime updatedAt;


    // =========================================================
    // ENDED
    // =========================================================

    private LocalDateTime endedAt;
}