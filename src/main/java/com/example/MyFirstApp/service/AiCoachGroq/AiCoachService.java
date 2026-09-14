package com.example.MyFirstApp.service.AiCoachGroq;


import com.example.MyFirstApp.DTO.AiCoach.AiCoachHistoryDTO;
import com.example.MyFirstApp.DTO.AiCoach.AiCoachMessageResponse;
import com.example.MyFirstApp.DTO.AiCoach.EndAiCoachResponse;
import com.example.MyFirstApp.DTO.AiCoach.StartAiCoachResponse;
import com.example.MyFirstApp.Entity.AiCoah.AiCoachMessage;
import com.example.MyFirstApp.Entity.AiCoah.AiCoachSession;
import com.example.MyFirstApp.Enum.AiCoachSessionStatus;


import com.example.MyFirstApp.prompt.AiCoach.AiCoachPrompt;
import com.example.MyFirstApp.repository.AiCoachSessionRepository.AiCoachMessageRepository;
import com.example.MyFirstApp.repository.AiCoachSessionRepository.AiCoachSessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCoachService {


    private final AiCoachSessionRepository sessionRepository;

    private final AiCoachMessageRepository messageRepository;

    private final AiCoachGroqService groqService;


    // =========================================================
    // START SESSION
    // =========================================================

    public StartAiCoachResponse startSession(

            Long userId,

            String coachMode

    ) {

        if (
                userId == null
        ) {

            throw new IllegalArgumentException(
                    "userId is required"
            );
        }


        String normalizedMode =
                normalizeMode(
                        coachMode
                );


        // =====================================================
        // CREATE SESSION
        // =====================================================

        AiCoachSession session =
                AiCoachSession.builder()

                        .userId(
                                userId
                        )

                        .title(
                                "AI Coach Session"
                        )

                        .coachMode(
                                normalizedMode
                        )

                        .status(
                                AiCoachSessionStatus.ACTIVE
                        )

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .updatedAt(
                                LocalDateTime.now()
                        )

                        .build();


        session =
                sessionRepository.save(
                        session
                );


        // =====================================================
        // FIRST AI MESSAGE
        // =====================================================

        String firstMessage =
                buildWelcomeMessage(
                        normalizedMode
                );


        saveMessage(
                session.getId(),
                "AI",
                firstMessage
        );


        // =====================================================
        // RESPONSE
        // =====================================================

        return StartAiCoachResponse
                .builder()

                .sessionId(
                        session.getId()
                )

                .title(
                        session.getTitle()
                )

                .coachMode(
                        session.getCoachMode()
                )

                .aiMessage(
                        firstMessage
                )

                .status(
                        session.getStatus()
                                .name()
                )

                .build();
    }


    // =========================================================
    // SEND MESSAGE
    // =========================================================

    public AiCoachMessageResponse sendMessage(

            Long sessionId,

            String userMessage

    ) {

        if (
                sessionId == null
        ) {

            throw new IllegalArgumentException(
                    "sessionId is required"
            );
        }


        if (
                userMessage == null
                        ||
                        userMessage.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Message cannot be empty"
            );
        }


        // =====================================================
        // LOAD SESSION
        // =====================================================

        AiCoachSession session =
                sessionRepository
                        .findById(
                                sessionId
                        )

                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "AI Coach session not found"
                                        )
                        );


        // =====================================================
        // STATUS
        // =====================================================

        if (
                session.getStatus()
                        != AiCoachSessionStatus.ACTIVE
        ) {

            throw new RuntimeException(
                    "AI Coach session is already completed"
            );
        }


        String cleanMessage =
                userMessage.trim();


        // =====================================================
        // GET HISTORY BEFORE CURRENT MESSAGE
        // =====================================================

        String history =
                buildConversationHistory(
                        sessionId
                );


        // =====================================================
        // SAVE USER MESSAGE
        // =====================================================

        saveMessage(
                sessionId,
                "USER",
                cleanMessage
        );


        // =====================================================
        // BUILD PROMPT
        // =====================================================

        String prompt =
                AiCoachPrompt.build(

                        session.getCoachMode(),

                        history,

                        cleanMessage
                );


        // =====================================================
        // ASK AI
        // =====================================================

        String aiResponse =
                groqService.askAI(
                        prompt
                );


        // =====================================================
        // SAVE AI
        // =====================================================

        saveMessage(
                sessionId,
                "AI",
                aiResponse
        );


        // =====================================================
        // UPDATE SESSION
        // =====================================================

        session.setUpdatedAt(
                LocalDateTime.now()
        );


        sessionRepository.save(
                session
        );


        // =====================================================
        // RESPONSE
        // =====================================================

        return AiCoachMessageResponse
                .builder()

                .sessionId(
                        sessionId
                )

                .userMessage(
                        cleanMessage
                )

                .aiMessage(
                        aiResponse
                )

                .sessionActive(
                        true
                )

                .build();
    }


    // =========================================================
    // HISTORY
    // =========================================================

    public List<AiCoachHistoryDTO> getHistory(

            Long sessionId

    ) {

        if (
                sessionId == null
        ) {

            throw new IllegalArgumentException(
                    "sessionId is required"
            );
        }


        if (
                !sessionRepository.existsById(
                        sessionId
                )
        ) {

            throw new RuntimeException(
                    "AI Coach session not found"
            );
        }


        List<AiCoachMessage> messages =
                messageRepository
                        .findBySessionIdOrderByCreatedAtAsc(
                                sessionId
                        );


        return messages
                .stream()
                .map(

                        message ->

                                AiCoachHistoryDTO
                                        .builder()

                                        .id(
                                                message.getId()
                                        )

                                        .sender(
                                                message.getSender()
                                        )

                                        .message(
                                                message.getMessage()
                                        )

                                        .createdAt(
                                                message.getCreatedAt()
                                        )

                                        .build()

                )
                .toList();
    }


    // =========================================================
    // END SESSION
    // =========================================================

    public EndAiCoachResponse endSession(

            Long sessionId

    ) {

        if (
                sessionId == null
        ) {

            throw new IllegalArgumentException(
                    "sessionId is required"
            );
        }


        AiCoachSession session =
                sessionRepository
                        .findById(
                                sessionId
                        )

                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "AI Coach session not found"
                                        )
                        );


        session.setStatus(
                AiCoachSessionStatus.COMPLETED
        );


        session.setEndedAt(
                LocalDateTime.now()
        );


        session.setUpdatedAt(
                LocalDateTime.now()
        );


        sessionRepository.save(
                session
        );


        return EndAiCoachResponse
                .builder()

                .sessionId(
                        sessionId
                )

                .status(
                        session.getStatus()
                                .name()
                )

                .message(
                        "AI Coach session completed successfully"
                )

                .build();
    }


    // =========================================================
    // GET USER SESSIONS
    // =========================================================

    public List<AiCoachSession> getUserSessions(

            Long userId

    ) {

        if (
                userId == null
        ) {

            throw new IllegalArgumentException(
                    "userId is required"
            );
        }


        return sessionRepository
                .findByUserIdOrderByUpdatedAtDesc(
                        userId
                );
    }


    // =========================================================
    // DELETE SESSION
    // =========================================================
    @Transactional
    public void deleteSession(
            Long sessionId
    ) {

        if (
                sessionId == null
        ) {

            throw new IllegalArgumentException(
                    "sessionId is required"
            );
        }

        AiCoachSession session =
                sessionRepository
                        .findById(sessionId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "AI Coach session not found"
                                        )
                        );

        int deletedMessages =
                messageRepository.deleteBySessionId(
                        sessionId
                );

        System.out.println(
                "🗑️ AI COACH MESSAGES DELETED: "
                        + deletedMessages
        );

        sessionRepository.delete(
                session
        );

        System.out.println(
                "🗑️ AI COACH SESSION DELETED: "
                        + sessionId
        );
    }



    // =========================================================
    // BUILD HISTORY
    // =========================================================

    private String buildConversationHistory(

            Long sessionId

    ) {

        List<AiCoachMessage> messages =
                messageRepository
                        .findBySessionIdOrderByCreatedAtAsc(
                                sessionId
                        );


        StringBuilder history =
                new StringBuilder();


        for (
                AiCoachMessage message
                : messages
        ) {

            history
                    .append(
                            message.getSender()
                    )

                    .append(
                            ": "
                    )

                    .append(
                            message.getMessage()
                    )

                    .append(
                            "\n"
                    );
        }


        return history.toString();
    }


    // =========================================================
    // SAVE MESSAGE
    // =========================================================

    private void saveMessage(

            Long sessionId,

            String sender,

            String message

    ) {

        AiCoachMessage entity =
                AiCoachMessage.builder()

                        .sessionId(
                                sessionId
                        )

                        .sender(
                                sender
                        )

                        .message(
                                message
                        )

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .build();


        messageRepository.save(
                entity
        );
    }


    // =========================================================
    // WELCOME MESSAGE
    // =========================================================

    private String buildWelcomeMessage(

            String mode

    ) {

        if (
                "INTERVIEW".equals(
                        mode
                )
        ) {

            return """
                    Hi! I'm your AI Coach.

                    I can help you prepare for HR interviews,
                    technical interviews, improve your answers,
                    and build interview confidence.

                    What would you like to practice today?
                    """;
        }


        if (
                "COMMUNICATION".equals(
                        mode
                )
        ) {

            return """
                    Hi! I'm your AI Communication Coach.

                    I can help you improve your English,
                    grammar, vocabulary, fluency and confidence.

                    What would you like to practice today?
                    """;
        }


        if (
                "CAREER".equals(
                        mode
                )
        ) {

            return """
                    Hi! I'm your AI Career Coach.

                    I can help you with career planning,
                    learning roadmaps, skills and job preparation.

                    What are you currently trying to achieve?
                    """;
        }


        if (
                "RESUME".equals(
                        mode
                )
        ) {

            return """
                    Hi! I'm your AI Resume Coach.

                    I can help you improve your resume,
                    projects, skills and interview preparation.

                    What would you like to improve?
                    """;
        }


        return """
                Hi! I'm your AI Coach.

                Think of me as your personal learning,
                career and interview mentor.

                Ask me anything and let's work on it together.
                """;
    }


    // =========================================================
    // NORMALIZE MODE
    // =========================================================

    private String normalizeMode(

            String mode

    ) {

        if (
                mode == null
                        ||
                        mode.isBlank()
        ) {

            return "GENERAL";
        }


        String normalized =
                mode
                        .trim()
                        .toUpperCase();


        if (
                normalized.equals("GENERAL")
                        ||
                        normalized.equals("INTERVIEW")
                        ||
                        normalized.equals("COMMUNICATION")
                        ||
                        normalized.equals("CAREER")
                        ||
                        normalized.equals("RESUME")
        ) {

            return normalized;
        }


        return "GENERAL";
    }
}