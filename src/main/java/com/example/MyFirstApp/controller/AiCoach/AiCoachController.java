package com.example.MyFirstApp.controller.AiCoach;



import com.example.MyFirstApp.DTO.AiCoach.*;
import com.example.MyFirstApp.Entity.AiCoah.AiCoachSession;
import com.example.MyFirstApp.service.AiCoachGroq.AiCoachService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai-coach")
@RequiredArgsConstructor
public class AiCoachController {


    private final AiCoachService aiCoachService;


    // =========================================================
    // START SESSION
    // =========================================================

    @PostMapping("/start")
    public ResponseEntity<StartAiCoachResponse> startSession(

            @RequestBody StartAiCoachRequest request

    ) {

        if (
                request == null
        ) {

            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }


        if (
                request.getUserId() == null
        ) {

            throw new IllegalArgumentException(
                    "userId is required"
            );
        }


        StartAiCoachResponse response =
                aiCoachService.startSession(

                        request.getUserId(),

                        request.getCoachMode()
                );


        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // SEND MESSAGE
    // =========================================================

    @PostMapping("/message")
    public ResponseEntity<AiCoachMessageResponse>
    sendMessage(

            @RequestBody AiCoahMessageRequest request

    ) {

        if (
                request == null
        ) {

            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }


        if (
                request.getSessionId() == null
        ) {

            throw new IllegalArgumentException(
                    "sessionId is required"
            );
        }


        if (
                request.getMessage() == null
                        ||
                        request.getMessage().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "message is required"
            );
        }


        AiCoachMessageResponse response =
                aiCoachService.sendMessage(

                        request.getSessionId(),

                        request.getMessage()
                );


        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // HISTORY
    // =========================================================

    @GetMapping("/{sessionId}/history")
    public ResponseEntity<List<AiCoachHistoryDTO>>
    getHistory(

            @PathVariable Long sessionId

    ) {

        return ResponseEntity.ok(
                aiCoachService.getHistory(
                        sessionId
                )
        );
    }


    // =========================================================
    // USER SESSIONS
    // =========================================================

    @GetMapping("/user/{userId}/sessions")
    public ResponseEntity<List<AiCoachSession>>
    getUserSessions(

            @PathVariable Long userId

    ) {

        return ResponseEntity.ok(
                aiCoachService.getUserSessions(
                        userId
                )
        );
    }


    // =========================================================
    // END SESSION
    // =========================================================

    @PostMapping("/end/{sessionId}")
    public ResponseEntity<EndAiCoachResponse>
    endSession(

            @PathVariable Long sessionId

    ) {

        return ResponseEntity.ok(
                aiCoachService.endSession(
                        sessionId
                )
        );
    }


    // =========================================================
    // DELETE SESSION
    // =========================================================

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void>
    deleteSession(

            @PathVariable Long sessionId

    ) {

        aiCoachService.deleteSession(
                sessionId
        );


        return ResponseEntity.noContent()
                .build();
    }
}