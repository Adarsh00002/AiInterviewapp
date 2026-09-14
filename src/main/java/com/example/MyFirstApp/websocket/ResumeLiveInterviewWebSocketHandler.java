package com.example.MyFirstApp.websocket;


import com.example.MyFirstApp.service.ResumeGroqService.ResumeGroqSTTService;
import com.example.MyFirstApp.service.ResumeInterview.ResumeLiveInterviewService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.Disposable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class ResumeLiveInterviewWebSocketHandler
        extends TextWebSocketHandler {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final ResumeGroqSTTService resumeGroqSTTService;

    private final ResumeLiveInterviewService resumeLiveInterviewService;


    // =========================================================
    // WEBSOCKET -> INTERVIEW SESSION
    // =========================================================

    private final Map<String, Long>
            interviewSessions =
            new ConcurrentHashMap<>();


    // =========================================================
    // WEBSOCKET SESSION
    // =========================================================

    private final Map<String, WebSocketSession>
            websocketSessions =
            new ConcurrentHashMap<>();


    // =========================================================
    // ANSWER PROCESSING
    // =========================================================

    private final Map<String, AtomicBoolean>
            answerProcessing =
            new ConcurrentHashMap<>();


    // =========================================================
    // ACTIVE AI
    // =========================================================

    private final Map<String, Disposable>
            activeAiProcesses =
            new ConcurrentHashMap<>();


    // =========================================================
    // AI SPEAKING
    // =========================================================

    private final Map<String, AtomicBoolean>
            aiSpeaking =
            new ConcurrentHashMap<>();


    // =========================================================
    // CONNECTION
    // =========================================================

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session
    ) {

        String websocketId =
                session.getId();

        websocketSessions.put(
                websocketId,
                session
        );

        answerProcessing.put(
                websocketId,
                new AtomicBoolean(false)
        );

        aiSpeaking.put(
                websocketId,
                new AtomicBoolean(false)
        );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "🔌 RESUME LIVE WEBSOCKET CONNECTED"
        );

        System.out.println(
                "WEBSOCKET: " + websocketId
        );

        System.out.println(
                "========================================"
        );
    }


    // =========================================================
    // TEXT MESSAGE
    // =========================================================

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {

        String websocketId =
                session.getId();

        try {

            JsonNode root =
                    objectMapper.readTree(
                            message.getPayload()
                    );

            String type =
                    root.path(
                            "type"
                    ).asText("");


            // =================================================
            // SESSION START
            // =================================================

            if (
                    "SESSION_START".equals(type)
            ) {

                handleSessionStart(
                        session,
                        root
                );

                return;
            }


            // =================================================
            // USER SPEAKING
            // =================================================

            if (
                    "USER_SPEAKING".equals(type)
            ) {

                handleUserSpeaking(
                        session
                );

                return;
            }


            // =================================================
            // END AUDIO
            // =================================================

            if (
                    "END_AUDIO".equals(type)
            ) {

                handleEndAudio(
                        session
                );

                return;
            }


            // =================================================
            // INTERRUPT
            // =================================================

            if (
                    "INTERRUPT_AI".equals(type)
                            || "CANCEL_AI".equals(type)
            ) {

                cancelAI(
                        session
                );

                return;
            }


            // =================================================
            // DISCONNECT
            // =================================================

            if (
                    "DISCONNECT_INTERVIEW".equals(type)
            ) {

                cancelAI(
                        session
                );

                return;
            }


            // =================================================
            // PING
            // =================================================

            if (
                    "PING".equals(type)
            ) {

                sendJson(
                        session,
                        Map.of(
                                "type",
                                "PONG"
                        )
                );

                return;
            }


            System.out.println(
                    "⚠️ UNKNOWN RESUME WS TYPE: "
                            + type
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    session,
                    e.getMessage()
            );
        }
    }


    // =========================================================
    // SESSION START
    // =========================================================

    private void handleSessionStart(
            WebSocketSession websocket,
            JsonNode root
    ) {

        String websocketId =
                websocket.getId();

        Long sessionId =
                extractSessionId(
                        root
                );

        if (
                sessionId == null
        ) {

            sendError(
                    websocket,
                    "sessionId is required"
            );

            return;
        }


        interviewSessions.put(
                websocketId,
                sessionId
        );


        answerProcessing
                .computeIfAbsent(
                        websocketId,
                        key ->
                                new AtomicBoolean(false)
                )
                .set(false);


        aiSpeaking
                .computeIfAbsent(
                        websocketId,
                        key ->
                                new AtomicBoolean(false)
                )
                .set(false);


        resumeGroqSTTService.connect(
                websocketId,
                (
                        transcriptType,
                        text
                ) -> {

                    handleTranscript(
                            websocketId,
                            transcriptType,
                            text
                    );
                }
        );


        sendJson(
                websocket,
                Map.of(
                        "type",
                        "SESSION_READY",
                        "sessionId",
                        sessionId
                )
        );


        // =====================================================
        // PLAY FIRST QUESTION
        // =====================================================

        sendJson(
                websocket,
                Map.of(
                        "type",
                        "AI_START"
                )
        );

        aiSpeaking
                .get(websocketId)
                .set(true);


        Disposable firstQuestionProcess =
                resumeLiveInterviewService
                        .playCurrentQuestion(

                                sessionId,

                                (
                                        text,
                                        audioUrl
                                ) -> {

                                    if (
                                            websocket.isOpen()
                                    ) {

                                        sendJson(
                                                websocket,
                                                Map.of(
                                                        "type",
                                                        "AI_AUDIO",
                                                        "text",
                                                        text,
                                                        "audioUrl",
                                                        audioUrl
                                                )
                                        );
                                    }
                                },

                                () -> {

                                    aiSpeaking
                                            .get(websocketId)
                                            .set(false);

                                    if (
                                            websocket.isOpen()
                                    ) {

                                        sendJson(
                                                websocket,
                                                Map.of(
                                                        "type",
                                                        "AI_COMPLETE"
                                                )
                                        );
                                    }
                                },

                                error -> {

                                    aiSpeaking
                                            .get(websocketId)
                                            .set(false);

                                    sendError(
                                            websocket,
                                            error == null
                                                    ? "Initial question audio failed"
                                                    : error.getMessage()
                                    );
                                }
                        );


        activeAiProcesses.put(
                websocketId,
                firstQuestionProcess
        );
    }


    // =========================================================
    // USER SPEAKING
    // =========================================================

    private void handleUserSpeaking(
            WebSocketSession websocket
    ) {

        String websocketId =
                websocket.getId();

        AtomicBoolean speaking =
                aiSpeaking.computeIfAbsent(
                        websocketId,
                        key ->
                                new AtomicBoolean(false)
                );


        // =====================================================
        // INTERRUPT AI
        // =====================================================

        if (
                speaking.get()
        ) {

            cancelAI(
                    websocket
            );
        }


        sendJson(
                websocket,
                Map.of(
                        "type",
                        "USER_TURN"
                )
        );
    }


    // =========================================================
    // END AUDIO
    // =========================================================

    private void handleEndAudio(
            WebSocketSession websocket
    ) {

        String websocketId =
                websocket.getId();

        if (
                interviewSessions.get(
                        websocketId
                ) == null
        ) {

            sendError(
                    websocket,
                    "Interview session is not mapped"
            );

            return;
        }


        AtomicBoolean processing =
                answerProcessing.computeIfAbsent(
                        websocketId,
                        key ->
                                new AtomicBoolean(false)
                );


        if (
                processing.get()
        ) {

            return;
        }


        resumeGroqSTTService.commitAudio(
                websocketId
        );
    }


    // =========================================================
    // BINARY PCM
    // =========================================================

    @Override
    protected void handleBinaryMessage(
            WebSocketSession session,
            BinaryMessage message
    ) {

        String websocketId =
                session.getId();

        Long sessionId =
                interviewSessions.get(
                        websocketId
                );

        if (
                sessionId == null
        ) {

            sendError(
                    session,
                    "Interview session not mapped"
            );

            return;
        }


        ByteBuffer buffer =
                message.getPayload();

        byte[] bytes =
                new byte[
                        buffer.remaining()
                        ];

        buffer.get(
                bytes
        );

        if (
                bytes.length == 0
        ) {

            return;
        }


        resumeGroqSTTService.sendAudio(
                websocketId,
                bytes
        );
    }


    // =========================================================
    // TRANSCRIPT
    // =========================================================

    private void handleTranscript(
            String websocketId,
            String transcriptType,
            String text
    ) {

        WebSocketSession websocket =
                websocketSessions.get(
                        websocketId
                );

        if (
                websocket == null
                        || !websocket.isOpen()
        ) {

            return;
        }


        // =====================================================
        // TOO SHORT
        // =====================================================

        if (
                "too_short".equals(
                        transcriptType
                )
        ) {

            sendJson(
                    websocket,
                    Map.of(
                            "type",
                            "TRANSCRIPT",
                            "transcriptType",
                            "too_short",
                            "text",
                            ""
                    )
            );

            answerProcessing
                    .get(
                            websocketId
                    )
                    .set(false);

            return;
        }


        // =====================================================
        // ERROR
        // =====================================================

        if (
                "error".equals(
                        transcriptType
                )
        ) {

            sendError(
                    websocket,
                    text
            );

            answerProcessing
                    .get(
                            websocketId
                    )
                    .set(false);

            return;
        }


        // =====================================================
        // TRANSCRIPT
        // =====================================================

        if (
                "committed".equals(
                        transcriptType
                )
        ) {

            if (
                    text == null
                            || text.isBlank()
            ) {

                return;
            }


            sendJson(
                    websocket,
                    Map.of(
                            "type",
                            "TRANSCRIPT",
                            "transcriptType",
                            "committed",
                            "text",
                            text
                    )
            );


            AtomicBoolean processing =
                    answerProcessing.computeIfAbsent(
                            websocketId,
                            key ->
                                    new AtomicBoolean(false)
                    );


            if (
                    !processing.compareAndSet(
                            false,
                            true
                    )
            ) {

                return;
            }


            Long sessionId =
                    interviewSessions.get(
                            websocketId
                    );


            if (
                    sessionId == null
            ) {

                processing.set(false);

                sendError(
                        websocket,
                        "Interview session not found"
                );

                return;
            }


            sendJson(
                    websocket,
                    Map.of(
                            "type",
                            "AI_START"
                    )
            );


            aiSpeaking
                    .get(websocketId)
                    .set(true);


            Disposable process =
                    resumeLiveInterviewService
                            .processAnswer(

                                    sessionId,

                                    text,

                                    // =====================
                                    // AI TEXT CHUNK
                                    // =====================

                                    chunk -> {

                                        // =====================================================
                                        // IMPORTANT
                                        // =====================================================
                                        //
                                        // AI text internally stream ho raha hai,
                                        // lekin frontend ko live caption ke liye
                                        // AI_CHUNK nahi bhejna hai.
                                        //
                                        // Frontend caption sirf ONE AI_AUDIO ke
                                        // playback progress se control karega.
                                        //
                                        // =====================================================

                                        System.out.println(
                                                "🤖 RESUME AI TEXT CHUNK: "
                                                        + chunk
                                        );
                                    },
                                    // =====================
                                    // AI AUDIO
                                    // =====================

                                    (
                                            textChunk,
                                            audioUrl
                                    ) -> {

                                        if (
                                                websocket.isOpen()
                                                        && textChunk != null
                                                        && !textChunk.isBlank()
                                                        && audioUrl != null
                                                        && !audioUrl.isBlank()
                                        ) {

                                            sendJson(
                                                    websocket,
                                                    Map.of(
                                                            "type",
                                                            "AI_AUDIO",
                                                            "text",
                                                            textChunk,
                                                            "audioUrl",
                                                            audioUrl
                                                    )
                                            );
                                        }
                                    },

                                    // =====================
                                    // COMPLETE
                                    // =====================

                                    () -> {

                                        processing.set(false);

                                        aiSpeaking
                                                .get(
                                                        websocketId
                                                )
                                                .set(false);

                                        activeAiProcesses.remove(
                                                websocketId
                                        );

                                        if (
                                                websocket.isOpen()
                                        ) {

                                            sendJson(
                                                    websocket,
                                                    Map.of(
                                                            "type",
                                                            "AI_COMPLETE"
                                                    )
                                            );
                                        }
                                    },

                                    // =====================
                                    // ERROR
                                    // =====================

                                    error -> {

                                        processing.set(false);

                                        aiSpeaking
                                                .get(
                                                        websocketId
                                                )
                                                .set(false);

                                        activeAiProcesses.remove(
                                                websocketId
                                        );

                                        sendError(
                                                websocket,
                                                error == null
                                                        ? "Resume live AI error"
                                                        : error.getMessage()
                                        );
                                    }
                            );


            activeAiProcesses.put(
                    websocketId,
                    process
            );
        }
    }


    // =========================================================
    // CANCEL AI
    // =========================================================

    private void cancelAI(
            WebSocketSession websocket
    ) {

        String websocketId =
                websocket.getId();

        Disposable process =
                activeAiProcesses.remove(
                        websocketId
                );

        if (
                process != null
        ) {

            try {

                process.dispose();

            } catch (Exception e) {

                System.err.println(
                        "❌ RESUME AI CANCEL ERROR: "
                                + e.getMessage()
                );
            }
        }


        resumeLiveInterviewService.cancelCurrentProcess(
                interviewSessions.get(
                        websocketId
                )
        );


        AtomicBoolean speaking =
                aiSpeaking.get(
                        websocketId
                );

        if (
                speaking != null
        ) {

            speaking.set(false);
        }


        AtomicBoolean processing =
                answerProcessing.get(
                        websocketId
                );

        if (
                processing != null
        ) {

            processing.set(false);
        }


        if (
                websocket.isOpen()
        ) {

            sendJson(
                    websocket,
                    Map.of(
                            "type",
                            "AI_INTERRUPTED"
                    )
            );
        }
    }


    // =========================================================
    // EXTRACT SESSION ID
    // =========================================================

    private Long extractSessionId(
            JsonNode root
    ) {

        if (
                root == null
        ) {

            return null;
        }

        JsonNode node =
                root.get(
                        "sessionId"
                );

        if (
                node == null
                        || node.isNull()
        ) {

            return null;
        }

        try {

            if (
                    node.isIntegralNumber()
            ) {

                return node.longValue();
            }

            String value =
                    node.asText();

            if (
                    value == null
                            || value.isBlank()
            ) {

                return null;
            }

            return Long.parseLong(
                    value
            );

        } catch (Exception e) {

            return null;
        }
    }


    // =========================================================
    // SEND JSON
    // =========================================================

    private void sendJson(
            WebSocketSession session,
            Object data
    ) {

        if (
                session == null
                        || !session.isOpen()
        ) {

            return;
        }

        try {

            String json =
                    objectMapper.writeValueAsString(
                            data
                    );

            synchronized (
                    session
            ) {

                if (
                        session.isOpen()
                ) {

                    session.sendMessage(
                            new TextMessage(
                                    json
                            )
                    );
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "❌ RESUME WS SEND ERROR: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // ERROR
    // =========================================================

    private void sendError(
            WebSocketSession session,
            String message
    ) {

        sendJson(
                session,
                Map.of(
                        "type",
                        "ERROR",
                        "message",
                        message == null
                                ? "Unknown error"
                                : message
                )
        );
    }


    // =========================================================
    // CLOSE
    // =========================================================

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        String websocketId =
                session.getId();

        try {

            Disposable process =
                    activeAiProcesses.remove(
                            websocketId
                    );

            if (
                    process != null
            ) {

                process.dispose();
            }

            resumeLiveInterviewService.cancelCurrentProcess(
                    interviewSessions.get(
                            websocketId
                    )
            );

            resumeGroqSTTService.close(
                    websocketId
            );

        } catch (Exception e) {

            System.err.println(
                    "❌ RESUME WS CLOSE ERROR: "
                            + e.getMessage()
            );
        }

        interviewSessions.remove(
                websocketId
        );

        websocketSessions.remove(
                websocketId
        );

        answerProcessing.remove(
                websocketId
        );

        activeAiProcesses.remove(
                websocketId
        );

        aiSpeaking.remove(
                websocketId
        );

        System.out.println(
                "🔌 RESUME LIVE WEBSOCKET CLOSED: "
                        + websocketId
        );
    }


    // =========================================================
    // TRANSPORT ERROR
    // =========================================================

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception
    ) {

        System.err.println(
                "❌ RESUME WEBSOCKET TRANSPORT ERROR: "
                        + (
                        exception == null
                                ? "Unknown"
                                : exception.getMessage()
                )
        );

        try {

            session.close(
                    CloseStatus.SERVER_ERROR
            );

        } catch (IOException e) {

            System.err.println(
                    "❌ RESUME WS CLOSE ERROR: "
                            + e.getMessage()
            );
        }
    }
}