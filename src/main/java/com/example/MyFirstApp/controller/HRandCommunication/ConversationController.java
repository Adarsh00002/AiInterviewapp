package com.example.MyFirstApp.controller.HRandCommunication;


import com.example.MyFirstApp.DTO.HRandCommunication.ConversationHistoryDTO;
import com.example.MyFirstApp.DTO.HRandCommunication.EndInterviewRequest;
import com.example.MyFirstApp.DTO.HRandCommunication.EndInterviewResponse;
import com.example.MyFirstApp.DTO.HRandCommunication.StartConversationRequest;
import com.example.MyFirstApp.DTO.HRandCommunication.StartConversationResponse;
import com.example.MyFirstApp.Entity.HRandCoummunicationEntity.ConversationMessage;
import com.example.MyFirstApp.Entity.HRandCoummunicationEntity.ConversationSession;
import com.example.MyFirstApp.Enum.SessionStatus;
import com.example.MyFirstApp.repository.HRandCoummunicationRespository.ConversationMessageRepositroy;
import com.example.MyFirstApp.repository.HRandCoummunicationRespository.ConversationSessionRepository;
import com.example.MyFirstApp.service.HRandCommunication.ConversationInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationSessionRepository sessionRepository;

    private final ConversationMessageRepositroy messageRepository;

    private final ConversationInterviewService conversationInterviewService;


    // =========================================================
    // CREATE CONVERSATION SESSION
    // =========================================================

    @PostMapping("/start")
    public ResponseEntity<StartConversationResponse> startConversation(
            @RequestBody StartConversationRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }

        if (request.getUserId() == null) {

            throw new IllegalArgumentException(
                    "userId is required"
            );
        }

        if (request.getMode() == null) {

            throw new IllegalArgumentException(
                    "mode is required"
            );
        }


        // =====================================================
        // CREATE DB SESSION
        // =====================================================

        ConversationSession session =
                ConversationSession.builder()

                        .userId(
                                request.getUserId()
                        )

                        .mode(
                                request.getMode()
                        )

                        .status(
                                SessionStatus.ACTIVE
                        )

                        .startedAt(
                                LocalDateTime.now()
                        )

                        .totalMessages(
                                0
                        )

                        .overallScore(
                                0.0
                        )

                        .build();


        ConversationSession savedSession =
                sessionRepository.save(
                        session
                );


        // =====================================================
        // RETURN SESSION ID
        //
        // IMPORTANT:
        //
        // AI conversation WebSocket se start hoga.
        //
        // REST API sirf session create karegi.
        // =====================================================

        StartConversationResponse response =
                StartConversationResponse.builder()

                        .sessionId(
                                savedSession.getId()
                        )

                        .aiMessage(
                                null
                        )

                        .audioUrl(
                                null
                        )

                        .status(
                                "SESSION_CREATED"
                        )

                        .build();


        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // GET CONVERSATION HISTORY
    // =========================================================

    @GetMapping("/{sessionId}/history")
    public ResponseEntity<List<ConversationHistoryDTO>> getHistory(
            @PathVariable Long sessionId
    ) {

        if (sessionId == null) {

            throw new IllegalArgumentException(
                    "sessionId is required"
            );
        }


        // =====================================================
        // CHECK SESSION
        // =====================================================

        if (
                !sessionRepository.existsById(
                        sessionId
                )
        ) {

            throw new RuntimeException(
                    "Conversation session not found"
            );
        }


        // =====================================================
        // GET MESSAGES
        // =====================================================

        List<ConversationMessage> messages =
                messageRepository
                        .findBySessionIdOrderByCreatedAtAsc(
                                sessionId
                        );


        // =====================================================
        // CONVERT ENTITY → DTO
        // =====================================================

        List<ConversationHistoryDTO> history =
                messages.stream()

                        .map(
                                message ->
                                        ConversationHistoryDTO
                                                .builder()

                                                .sender(
                                                        message
                                                                .getSender()
                                                                .name()
                                                )

                                                .message(
                                                        message
                                                                .getMessage()
                                                )

                                                .build()
                        )

                        .toList();


        return ResponseEntity.ok(
                history
        );
    }


    // =========================================================
    // END INTERVIEW
    // =========================================================

    @PostMapping("/end")
    public ResponseEntity<EndInterviewResponse> endInterview(
            @RequestBody EndInterviewRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }

        if (request.getSessionId() == null) {

            throw new IllegalArgumentException(
                    "sessionId is required"
            );
        }


        EndInterviewResponse response =
                conversationInterviewService
                        .endInterview(
                                request.getSessionId()
                        );


        return ResponseEntity.ok(
                response
        );
    }
}