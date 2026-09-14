
        package com.example.MyFirstApp.websocket;

import com.example.MyFirstApp.service.GroqService.GroqSTTService;
import com.example.MyFirstApp.service.VoiceInterviewService.LiveInterviewService;
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
public class LiveInterviewWebSocketHandler
        extends TextWebSocketHandler {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final GroqSTTService groqSTTService;

    private final LiveInterviewService liveInterviewService;

    // =========================================================
    // WEBSOCKET -> INTERVIEW SESSION
    // =========================================================

    private final Map<String, Long> interviewSessions =
            new ConcurrentHashMap<>();

    // =========================================================
    // WEBSOCKET ID -> WEBSOCKET SESSION
    // =========================================================

    private final Map<String, WebSocketSession> websocketSessions =
            new ConcurrentHashMap<>();

    // =========================================================
    // ANSWER PROCESSING
    // =========================================================

    private final Map<String, AtomicBoolean> answerProcessing =
            new ConcurrentHashMap<>();

    // =========================================================
    // ACTIVE AI PROCESS
    // =========================================================

    private final Map<String, Disposable> activeAiProcesses =
            new ConcurrentHashMap<>();


    // =========================================================
    // CONNECTION ESTABLISHED
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

        System.out.println(
                "======================================"
        );

        System.out.println(
                "🔌 LIVE INTERVIEW WEBSOCKET CONNECTED"
        );

        System.out.println(
                "WEBSOCKET SESSION: "
                        + websocketId
        );

        System.out.println(
                "======================================"
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

        String payload =
                message.getPayload();

        System.out.println(
                "======================================"
        );

        System.out.println(
                "📩 WS TEXT RECEIVED"
        );

        System.out.println(
                payload
        );

        System.out.println(
                "WEBSOCKET: "
                        + websocketId
        );

        System.out.println(
                "======================================"
        );

        try {

            JsonNode root =
                    objectMapper.readTree(
                            payload
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

                handleSessionMapping(
                        session,
                        root
                );

                return;
            }


            // =================================================
            // OLD SESSION FORMAT
            // =================================================

            if (
                    root.has("sessionId")
                            &&
                            !root.has("type")
            ) {

                handleSessionMapping(
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

                System.out.println(
                        "======================================"
                );

                System.out.println(
                        "🎤 USER SPEAKING"
                );

                System.out.println(
                        "======================================"
                );

                sendJson(
                        session,
                        Map.of(
                                "type",
                                "USER_TURN"
                        )
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
            // INTERRUPT AI
            // =================================================

            if (
                    "INTERRUPT_AI".equals(type)
                            ||
                            "CANCEL_AI".equals(type)
            ) {

                handleInterruptAI(
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

                System.out.println(
                        "======================================"
                );

                System.out.println(
                        "🔌 DISCONNECT_INTERVIEW RECEIVED"
                );

                System.out.println(
                        "======================================"
                );

                cancelActiveAI(
                        websocketId
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


            // =================================================
            // UNKNOWN
            // =================================================

            System.out.println(
                    "⚠️ UNKNOWN WS MESSAGE TYPE: "
                            + type
            );

        } catch (Exception e) {

            System.err.println(
                    "======================================"
            );

            System.err.println(
                    "❌ WS TEXT MESSAGE ERROR"
            );

            System.err.println(
                    e.getMessage()
            );

            System.err.println(
                    "======================================"
            );

            sendError(
                    session,
                    e.getMessage()
            );
        }
    }


    // =========================================================
    // SESSION MAPPING
    // =========================================================

    private void handleSessionMapping(
            WebSocketSession session,
            JsonNode root
    ) {

        String websocketId =
                session.getId();

        Long sessionId =
                extractSessionId(
                        root
                );

        if (
                sessionId == null
        ) {

            sendError(
                    session,
                    "sessionId is required"
            );

            return;
        }

        // =====================================================
        // SAVE SESSION MAPPING
        // =====================================================

        interviewSessions.put(
                websocketId,
                sessionId
        );


        // =====================================================
        // RESET ANSWER PROCESSING
        // =====================================================

        answerProcessing
                .computeIfAbsent(
                        websocketId,
                        key ->
                                new AtomicBoolean(false)
                )
                .set(false);


        // =====================================================
        // CREATE GROQ STT SESSION
        // =====================================================

        groqSTTService.connect(
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


        System.out.println(
                "======================================"
        );

        System.out.println(
                "✅ INTERVIEW SESSION MAPPED"
        );

        System.out.println(
                "WEBSOCKET: "
                        + websocketId
        );

        System.out.println(
                "INTERVIEW SESSION: "
                        + sessionId
        );

        System.out.println(
                "======================================"
        );


        // =====================================================
        // SESSION READY
        // =====================================================

        sendJson(
                session,
                Map.of(
                        "type",
                        "SESSION_READY"
                )
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
                    "Interview session is not mapped"
            );

            return;
        }


        ByteBuffer buffer =
                message.getPayload();

        byte[] audioBytes =
                new byte[
                        buffer.remaining()
                        ];

        buffer.get(
                audioBytes
        );


        if (
                audioBytes.length == 0
        ) {

            return;
        }


        System.out.println(
                "🎤 PCM RECEIVED: "
                        + audioBytes.length
                        + " bytes"
        );


        groqSTTService.sendAudio(
                websocketId,
                audioBytes
        );
    }


    // =========================================================
    // END USER AUDIO
    // =========================================================

    private void handleEndAudio(
            WebSocketSession session
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

            System.out.println(
                    "⚠️ ANSWER ALREADY PROCESSING"
            );

            return;
        }


        System.out.println(
                "======================================"
        );

        System.out.println(
                "🛑 END_AUDIO RECEIVED"
        );

        System.out.println(
                "WEBSOCKET: "
                        + websocketId
        );

        System.out.println(
                "SESSION: "
                        + sessionId
        );

        System.out.println(
                "======================================"
        );


        // =====================================================
        // COMMIT AUDIO TO STT
        // =====================================================

        groqSTTService.commitAudio(
                websocketId
        );
    }


    // =========================================================
    // INTERRUPT AI
    // =========================================================

    private void handleInterruptAI(
            WebSocketSession session
    ) {

        String websocketId =
                session.getId();

        System.out.println(
                "======================================"
        );

        System.out.println(
                "🛑 INTERRUPT AI"
        );

        System.out.println(
                "WEBSOCKET: "
                        + websocketId
        );

        System.out.println(
                "======================================"
        );


        cancelActiveAI(
                websocketId
        );


        sendJson(
                session,
                Map.of(
                        "type",
                        "AI_CANCELLED"
                )
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
                        ||
                        !websocket.isOpen()
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

            return;
        }


        // =====================================================
        // NON COMMITTED
        // =====================================================

        if (
                !"committed".equals(
                        transcriptType
                )
        ) {

            sendJson(
                    websocket,
                    Map.of(
                            "type",
                            "TRANSCRIPT",
                            "transcriptType",
                            transcriptType,
                            "text",
                            text == null
                                    ? ""
                                    : text
                    )
            );

            return;
        }


        // =====================================================
        // EMPTY TRANSCRIPT
        // =====================================================

        if (
                text == null
                        || text.isBlank()
        ) {

            return;
        }


        // =====================================================
        // USER TRANSCRIPT
        // =====================================================

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


        // =====================================================
        // DUPLICATE ANSWER PROTECTION
        // =====================================================

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

            System.out.println(
                    "⚠️ ANSWER ALREADY PROCESSING"
            );

            return;
        }


        // =====================================================
        // SESSION ID
        // =====================================================

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


        // =====================================================
        // AI START
        // =====================================================

        sendJson(
                websocket,
                Map.of(
                        "type",
                        "AI_START"
                )
        );


        System.out.println(
                "======================================"
        );

        System.out.println(
                "🧠 PROCESSING USER ANSWER"
        );

        System.out.println(
                "SESSION: "
                        + sessionId
        );

        System.out.println(
                "ANSWER: "
                        + text
        );

        System.out.println(
                "======================================"
        );


        // =====================================================
        // START LIVE AI PROCESS
        // =====================================================

        Disposable disposable =
                liveInterviewService.processAnswer(

                        sessionId,

                        text,

                        // =====================================
                        // AI TEXT CHUNK
                        // =====================================

                        chunk -> {

                            if (
                                    chunk == null
                                            || chunk.isBlank()
                            ) {

                                return;
                            }


                            sendJson(
                                    websocket,
                                    Map.of(
                                            "type",
                                            "AI_CHUNK",
                                            "text",
                                            chunk
                                    )
                            );
                        },


                        // =====================================
                        // AI TEXT + AUDIO
                        // =====================================
                        //
                        // VERY IMPORTANT:
                        //
                        // LiveInterviewService now expects:
                        //
                        // BiConsumer<String, String>
                        //
                        // first parameter  = textChunk
                        // second parameter = audioUrl
                        //
                        // =====================================

                        (
                                textChunk,
                                audioUrl
                        ) -> {

                            if (
                                    textChunk == null
                                            || textChunk.isBlank()
                                            || audioUrl == null
                                            || audioUrl.isBlank()
                            ) {

                                return;
                            }


                            System.out.println(
                                    "======================================"
                            );

                            System.out.println(
                                    "🔊 AI AUDIO + CAPTION READY"
                            );

                            System.out.println(
                                    "TEXT: "
                                            + textChunk
                            );

                            System.out.println(
                                    "AUDIO: "
                                            + audioUrl
                            );

                            System.out.println(
                                    "======================================"
                            );


                            // =================================================
                            // SEND TEXT + AUDIO TO FRONTEND
                            // =================================================

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
                        },


                        // =====================================
                        // COMPLETE
                        // =====================================

                        () -> {

                            processing.set(false);

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


                            System.out.println(
                                    "======================================"
                            );

                            System.out.println(
                                    "✅ AI RESPONSE + TTS COMPLETE"
                            );

                            System.out.println(
                                    "SESSION: "
                                            + sessionId
                            );

                            System.out.println(
                                    "======================================"
                            );
                        },


                        // =====================================
                        // ERROR
                        // =====================================

                        error -> {

                            processing.set(false);

                            activeAiProcesses.remove(
                                    websocketId
                            );


                            System.err.println(
                                    "======================================"
                            );

                            System.err.println(
                                    "❌ LIVE AI PROCESS ERROR"
                            );

                            System.err.println(
                                    error == null
                                            ? "Unknown error"
                                            : error.getMessage()
                            );

                            System.err.println(
                                    "======================================"
                            );


                            sendError(
                                    websocket,
                                    error == null
                                            ? "Live AI processing failed"
                                            : error.getMessage()
                            );
                        }
                );


        // =====================================================
        // SAVE ACTIVE PROCESS
        // =====================================================

        activeAiProcesses.put(
                websocketId,
                disposable
        );


        System.out.println(
                "✅ ACTIVE AI PROCESS REGISTERED"
        );
    }


    // =========================================================
    // CANCEL ACTIVE AI
    // =========================================================

    private void cancelActiveAI(
            String websocketId
    ) {

        Disposable disposable =
                activeAiProcesses.remove(
                        websocketId
                );


        if (
                disposable != null
        ) {

            try {

                disposable.dispose();

            } catch (Exception e) {

                System.err.println(
                        "⚠️ AI DISPOSE ERROR: "
                                + e.getMessage()
                );
            }
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
                    "❌ WS SEND ERROR: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // SEND ERROR
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
    // CONNECTION CLOSED
    // =========================================================

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        String websocketId =
                session.getId();


        System.out.println(
                "======================================"
        );

        System.out.println(
                "🔌 LIVE INTERVIEW WEBSOCKET CLOSED"
        );

        System.out.println(
                "WEBSOCKET SESSION: "
                        + websocketId
        );

        System.out.println(
                "STATUS: "
                        + status
        );

        System.out.println(
                "======================================"
        );


        // =====================================================
        // CANCEL AI
        // =====================================================

        cancelActiveAI(
                websocketId
        );


        // =====================================================
        // CLOSE STT
        // =====================================================

        try {

            groqSTTService.close(
                    websocketId
            );

        } catch (Exception e) {

            System.err.println(
                    "⚠️ GROQ STT CLOSE ERROR: "
                            + e.getMessage()
            );
        }


        // =====================================================
        // REMOVE STATE
        // =====================================================

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
                "======================================"
        );

        System.err.println(
                "❌ LIVE WEBSOCKET TRANSPORT ERROR"
        );

        System.err.println(
                "WEBSOCKET: "
                        + session.getId()
        );

        System.err.println(
                exception == null
                        ? "Unknown error"
                        : exception.getMessage()
        );

        System.err.println(
                "======================================"
        );


        try {

            session.close(
                    CloseStatus.SERVER_ERROR
            );

        } catch (IOException e) {

            System.err.println(
                    "❌ WS CLOSE ERROR: "
                            + e.getMessage()
            );
        }
    }
}

