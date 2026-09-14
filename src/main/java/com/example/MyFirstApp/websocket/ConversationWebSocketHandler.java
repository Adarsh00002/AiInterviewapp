package com.example.MyFirstApp.websocket;

import com.example.MyFirstApp.service.HRandCommunication.ConversationGroqSTTService;
import com.example.MyFirstApp.service.HRandCommunication.ConversationInterviewService;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class ConversationWebSocketHandler
        extends TextWebSocketHandler {


    // =========================================================
    // OBJECT MAPPER
    // =========================================================

    private final ObjectMapper objectMapper =
            new ObjectMapper();


    // =========================================================
    // SERVICES
    // =========================================================

    private final ConversationGroqSTTService
            conversationGroqSTTService;


    private final ConversationInterviewService
            conversationInterviewService;


    // =========================================================
    // WEBSOCKET SESSION MAP
    // =========================================================

    private final Map<String, WebSocketSession>
            websocketSessions =
            new ConcurrentHashMap<>();


    // =========================================================
    // CONVERSATION SESSION MAP
    // =========================================================

    private final Map<String, Long>
            conversationSessions =
            new ConcurrentHashMap<>();


    // =========================================================
    // ACTIVE AI PROCESS
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
    // ANSWER PROCESSING
    // =========================================================

    private final Map<String, AtomicBoolean>
            answerProcessing =
            new ConcurrentHashMap<>();


    // =========================================================
    // USER AUDIO BUFFER
    //
    // IMPORTANT:
    //
    // Frontend PCM chunks ko yahan temporarily collect karenge.
    //
    // PCM:
    //
    // 16kHz
    // Mono
    // 16-bit
    //
    // =========================================================

    private final Map<String, ByteArrayOutputStream>
            audioBuffers =
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


        aiSpeaking.put(
                websocketId,
                new AtomicBoolean(false)
        );


        answerProcessing.put(
                websocketId,
                new AtomicBoolean(false)
        );


        audioBuffers.put(
                websocketId,
                new ByteArrayOutputStream()
        );


        System.out.println(
                "========================================"
        );

        System.out.println(
                "🔌 CONVERSATION WEBSOCKET CONNECTED"
        );

        System.out.println(
                "WEBSOCKET ID: "
                        + websocketId
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

        if (
                session == null ||
                        !session.isOpen()
        ) {

            return;
        }


        String websocketId =
                session.getId();


        try {

            String payload =
                    message.getPayload();


            if (
                    payload == null ||
                            payload.isBlank()
            ) {

                sendError(
                        session,
                        "Empty WebSocket message"
                );

                return;
            }


            JsonNode root =
                    objectMapper.readTree(
                            payload
                    );


            if (
                    root == null ||
                            !root.isObject()
            ) {

                sendError(
                        session,
                        "Invalid WebSocket JSON"
                );

                return;
            }


            String type =
                    root.path(
                            "type"
                    ).asText("");


            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "📩 CONVERSATION WS MESSAGE: "
                            + type
            );

            System.out.println(
                    "WEBSOCKET: "
                            + websocketId
            );

            System.out.println(
                    "========================================"
            );


            // =================================================
            // SESSION START
            // =================================================

            if (
                    "SESSION_START".equalsIgnoreCase(
                            type
                    )
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
                    "USER_SPEAKING".equalsIgnoreCase(
                            type
                    )
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
                    "END_AUDIO".equalsIgnoreCase(
                            type
                    )
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
                    "INTERRUPT_AI".equalsIgnoreCase(
                            type
                    )
                            ||
                            "CANCEL_AI".equalsIgnoreCase(
                                    type
                            )
            ) {

                cancelAI(
                        session,
                        true
                );

                return;
            }


            // =================================================
            // END INTERVIEW
            // =================================================

            if (
                    "END_INTERVIEW".equalsIgnoreCase(
                            type
                    )
            ) {

                handleEndInterview(
                        session
                );

                return;
            }


            // =================================================
            // DISCONNECT
            // =================================================

            if (
                    "DISCONNECT_INTERVIEW".equalsIgnoreCase(
                            type
                    )
            ) {

                cancelAI(
                        session,
                        false
                );

                clearAudioBuffer(
                        websocketId
                );

                return;
            }


            // =================================================
            // PING
            // =================================================

            if (
                    "PING".equalsIgnoreCase(
                            type
                    )
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
                    "⚠️ UNKNOWN WS TYPE: "
                            + type
            );


            sendError(
                    session,
                    "Unknown WebSocket message type: "
                            + type
            );


        } catch (Exception e) {

            System.err.println(
                    "❌ CONVERSATION WS MESSAGE ERROR: "
                            + e.getMessage()
            );


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


        // =====================================================
        // CANCEL OLD AI
        // =====================================================

        Disposable oldProcess =
                activeAiProcesses.remove(
                        websocketId
                );


        if (
                oldProcess != null
        ) {

            try {

                oldProcess.dispose();

            } catch (Exception e) {

                System.err.println(
                        "⚠️ OLD AI PROCESS DISPOSE ERROR: "
                                + e.getMessage()
                );
            }
        }


        // =====================================================
        // SESSION MAP
        // =====================================================

        conversationSessions.put(
                websocketId,
                sessionId
        );


        // =====================================================
        // RESET BUFFER
        // =====================================================

        clearAudioBuffer(
                websocketId
        );


        // =====================================================
        // RESET STATES
        // =====================================================

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


        // =====================================================
        // SESSION READY
        // =====================================================

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
        // AI START
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


        // =====================================================
        // FIRST AI MESSAGE
        // =====================================================

        try {

            Disposable process =
                    conversationInterviewService
                            .startConversation(

                                    sessionId,

                                    (
                                            text,
                                            audioUrl
                                    ) -> {

                                        if (
                                                websocket == null ||
                                                        !websocket.isOpen()
                                        ) {

                                            return;
                                        }


                                        if (
                                                text == null ||
                                                        text.isBlank()
                                        ) {

                                            return;
                                        }


                                        if (
                                                audioUrl == null ||
                                                        audioUrl.isBlank()
                                        ) {

                                            return;
                                        }


                                        Map<String, Object>
                                                response =
                                                new HashMap<>();


                                        response.put(
                                                "type",
                                                "AI_AUDIO"
                                        );


                                        response.put(
                                                "text",
                                                text
                                        );


                                        response.put(
                                                "audioUrl",
                                                audioUrl
                                        );


                                        sendJson(
                                                websocket,
                                                response
                                        );
                                    },


                                    () -> {

                                        aiSpeaking
                                                .computeIfAbsent(
                                                        websocketId,
                                                        key ->
                                                                new AtomicBoolean(false)
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


                                    error -> {

                                        aiSpeaking
                                                .computeIfAbsent(
                                                        websocketId,
                                                        key ->
                                                                new AtomicBoolean(false)
                                                )
                                                .set(false);


                                        activeAiProcesses.remove(
                                                websocketId
                                        );


                                        sendError(
                                                websocket,
                                                error == null
                                                        ? "Initial AI processing failed"
                                                        : error.getMessage()
                                        );
                                    }
                            );


            if (
                    process != null
            ) {

                activeAiProcesses.put(
                        websocketId,
                        process
                );
            }


        } catch (Exception e) {

            aiSpeaking
                    .computeIfAbsent(
                            websocketId,
                            key ->
                                    new AtomicBoolean(false)
                    )
                    .set(false);


            System.err.println(
                    "❌ FIRST AI PROCESS ERROR: "
                            + e.getMessage()
            );


            sendError(
                    websocket,
                    e.getMessage()
            );
        }


        System.out.println(
                "🚀 CONVERSATION SESSION STARTED: "
                        + sessionId
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


        Long sessionId =
                conversationSessions.get(
                        websocketId
                );


        if (
                sessionId == null
        ) {

            sendError(
                    websocket,
                    "Conversation session not mapped"
            );

            return;
        }


        // =====================================================
        // CLEAR PREVIOUS AUDIO
        // =====================================================

        clearAudioBuffer(
                websocketId
        );


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

            System.out.println(
                    "🛑 USER INTERRUPTED AI"
            );


            cancelAI(
                    websocket,
                    true
            );
        }


        // =====================================================
        // USER TURN
        // =====================================================

        sendJson(
                websocket,
                Map.of(
                        "type",
                        "USER_TURN"
                )
        );


        System.out.println(
                "🎤 USER TURN STARTED: "
                        + sessionId
        );
    }


    // =========================================================
    // BINARY AUDIO
    // =========================================================

    @Override
    protected void handleBinaryMessage(

            WebSocketSession session,

            BinaryMessage message

    ) {

        if (
                session == null ||
                        !session.isOpen()
        ) {

            return;
        }


        String websocketId =
                session.getId();


        Long sessionId =
                conversationSessions.get(
                        websocketId
                );


        if (
                sessionId == null
        ) {

            sendError(
                    session,
                    "Conversation session not mapped"
            );

            return;
        }


        ByteBuffer buffer =
                message.getPayload();


        if (
                buffer == null
        ) {

            return;
        }


        int remaining =
                buffer.remaining();


        if (
                remaining <= 0
        ) {

            return;
        }


        byte[] bytes =
                new byte[remaining];


        buffer.get(
                bytes
        );


        try {

            ByteArrayOutputStream audioBuffer =
                    audioBuffers.computeIfAbsent(
                            websocketId,
                            key ->
                                    new ByteArrayOutputStream()
                    );


            synchronized (
                    audioBuffer
            ) {

                audioBuffer.write(
                        bytes
                );
            }


            System.out.println(
                    "🎤 PCM CHUNK RECEIVED: "
                            + bytes.length
                            + " bytes"
            );


            System.out.println(
                    "🎤 TOTAL PCM BUFFER: "
                            + audioBuffer.size()
                            + " bytes"
            );


        } catch (IOException e) {

            System.err.println(
                    "❌ PCM BUFFER ERROR: "
                            + e.getMessage()
            );


            sendError(
                    session,
                    "Unable to store microphone audio"
            );
        }
    }


    // =========================================================
    // END AUDIO
    // =========================================================

    private void handleEndAudio(
            WebSocketSession websocket
    ) {

        String websocketId =
                websocket.getId();


        Long sessionId =
                conversationSessions.get(
                        websocketId
                );


        if (
                sessionId == null
        ) {

            sendError(
                    websocket,
                    "Conversation session not mapped"
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
        // COPY AUDIO
        // =====================================================

        byte[] pcmBytes =
                getAudioBytes(
                        websocketId
                );


        // =====================================================
        // CLEAR BUFFER IMMEDIATELY
        // =====================================================

        clearAudioBuffer(
                websocketId
        );


        if (
                pcmBytes.length == 0
        ) {

            processing.set(false);


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


        System.out.println(
                "========================================"
        );

        System.out.println(
                "🎤 COMMITTING CONVERSATION AUDIO"
        );

        System.out.println(
                "SESSION ID: "
                        + sessionId
        );

        System.out.println(
                "PCM BYTES: "
                        + pcmBytes.length
        );

        System.out.println(
                "========================================"
        );


        // =====================================================
        // STT ASYNC
        // =====================================================

        CompletableFuture
                .supplyAsync(
                        () -> {

                            try {

                                /*
                                 * IMPORTANT:
                                 *
                                 * ConversationGroqSTTService
                                 * ka existing transcribe(...)
                                 * method use ho raha hai.
                                 *
                                 * Yahan RAW PCM ja raha hai.
                                 *
                                 * Tumhare STT service ke andar:
                                 *
                                 * RAW PCM
                                 *      ↓
                                 * PCM -> WAV
                                 *      ↓
                                 * Groq STT
                                 *
                                 * hona chahiye.
                                 */

                                return conversationGroqSTTService
                                        .transcribe(
                                                pcmBytes
                                        );

                            } catch (Exception e) {

                                throw new RuntimeException(
                                        e
                                );
                            }
                        }
                )
                .whenComplete(
                        (
                                transcript,
                                throwable
                        ) -> {

                            if (
                                    throwable != null
                            ) {

                                processing.set(false);


                                Throwable cause =
                                        throwable.getCause() != null
                                                ? throwable.getCause()
                                                : throwable;


                                System.err.println(
                                        "❌ CONVERSATION GROQ STT ERROR: "
                                                + cause.getMessage()
                                );


                                sendError(
                                        websocket,
                                        cause.getMessage()
                                );


                                return;
                            }


                            handleTranscribedAnswer(
                                    websocket,
                                    sessionId,
                                    transcript,
                                    processing
                            );
                        }
                );
    }


    // =========================================================
    // HANDLE TRANSCRIPT
    // =========================================================

    private void handleTranscribedAnswer(

            WebSocketSession websocket,

            Long sessionId,

            String transcript,

            AtomicBoolean processing

    ) {

        String cleanTranscript =
                transcript == null
                        ? ""
                        : transcript.trim();


        // =====================================================
        // EMPTY
        // =====================================================

        if (
                cleanTranscript.isBlank()
        ) {

            processing.set(false);


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


            System.out.println(
                    "⚠️ EMPTY TRANSCRIPT"
            );


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
                        cleanTranscript
                )
        );


        System.out.println(
                "========================================"
        );

        System.out.println(
                "🎤 USER TRANSCRIPT"
        );

        System.out.println(
                cleanTranscript
        );

        System.out.println(
                "========================================"
        );


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


        aiSpeaking
                .computeIfAbsent(
                        websocket.getId(),
                        key ->
                                new AtomicBoolean(false)
                )
                .set(true);


        // =====================================================
        // AI RESPONSE
        // =====================================================

        try {

            Disposable process =
                    conversationInterviewService
                            .processUserMessage(

                                    sessionId,

                                    cleanTranscript,


                                    // ==================================
                                    // AI CHUNK
                                    // ==================================

                                    chunk -> {

                                        if (
                                                websocket.isOpen() &&
                                                        chunk != null &&
                                                        !chunk.isBlank()
                                        ) {

                                            sendJson(
                                                    websocket,
                                                    Map.of(
                                                            "type",
                                                            "AI_CHUNK",
                                                            "text",
                                                            chunk
                                                    )
                                            );
                                        }
                                    },


                                    // ==================================
                                    // AI AUDIO
                                    // ==================================

                                    (
                                            textChunk,
                                            audioUrl
                                    ) -> {

                                        if (
                                                !websocket.isOpen()
                                        ) {

                                            return;
                                        }


                                        if (
                                                textChunk == null ||
                                                        textChunk.isBlank()
                                        ) {

                                            return;
                                        }


                                        if (
                                                audioUrl == null ||
                                                        audioUrl.isBlank()
                                        ) {

                                            return;
                                        }


                                        Map<String, Object>
                                                response =
                                                new HashMap<>();


                                        response.put(
                                                "type",
                                                "AI_AUDIO"
                                        );


                                        response.put(
                                                "text",
                                                textChunk
                                        );


                                        response.put(
                                                "audioUrl",
                                                audioUrl
                                        );


                                        sendJson(
                                                websocket,
                                                response
                                        );
                                    },


                                    // ==================================
                                    // COMPLETE
                                    // ==================================

                                    () -> {

                                        processing.set(false);


                                        aiSpeaking
                                                .computeIfAbsent(
                                                        websocket.getId(),
                                                        key ->
                                                                new AtomicBoolean(false)
                                                )
                                                .set(false);


                                        activeAiProcesses.remove(
                                                websocket.getId()
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
                                                "✅ AI RESPONSE COMPLETE"
                                        );
                                    },


                                    // ==================================
                                    // ERROR
                                    // ==================================

                                    error -> {

                                        processing.set(false);


                                        aiSpeaking
                                                .computeIfAbsent(
                                                        websocket.getId(),
                                                        key ->
                                                                new AtomicBoolean(false)
                                                )
                                                .set(false);


                                        activeAiProcesses.remove(
                                                websocket.getId()
                                        );


                                        System.err.println(
                                                "❌ CONVERSATION AI ERROR: "
                                                        +
                                                        (
                                                                error == null
                                                                        ? "Unknown error"
                                                                        : error.getMessage()
                                                        )
                                        );


                                        sendError(
                                                websocket,
                                                error == null
                                                        ? "Conversation AI processing failed"
                                                        : error.getMessage()
                                        );
                                    }
                            );


            if (
                    process != null
            ) {

                activeAiProcesses.put(
                        websocket.getId(),
                        process
                );
            }


        } catch (Exception e) {

            processing.set(false);


            aiSpeaking
                    .computeIfAbsent(
                            websocket.getId(),
                            key ->
                                    new AtomicBoolean(false)
                    )
                    .set(false);


            System.err.println(
                    "❌ PROCESS USER MESSAGE ERROR: "
                            + e.getMessage()
            );


            sendError(
                    websocket,
                    e.getMessage()
            );
        }
    }


    // =========================================================
    // GET AUDIO BYTES
    // =========================================================

    private byte[] getAudioBytes(
            String websocketId
    ) {

        ByteArrayOutputStream buffer =
                audioBuffers.get(
                        websocketId
                );


        if (
                buffer == null
        ) {

            return new byte[0];
        }


        synchronized (
                buffer
        ) {

            return buffer.toByteArray();
        }
    }


    // =========================================================
    // CLEAR AUDIO BUFFER
    // =========================================================

    private void clearAudioBuffer(
            String websocketId
    ) {

        ByteArrayOutputStream oldBuffer =
                audioBuffers.remove(
                        websocketId
                );


        if (
                oldBuffer != null
        ) {

            try {

                oldBuffer.close();

            } catch (IOException e) {

                // ignore
            }
        }


        audioBuffers.put(
                websocketId,
                new ByteArrayOutputStream()
        );
    }


    // =========================================================
    // CANCEL AI
    // =========================================================

    private void cancelAI(

            WebSocketSession websocket,

            boolean notifyClient

    ) {

        String websocketId =
                websocket.getId();


        // =====================================================
        // CANCEL REACTIVE AI
        // =====================================================

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
                        "⚠️ AI PROCESS CANCEL ERROR: "
                                + e.getMessage()
                );
            }
        }


        // =====================================================
        // SERVICE CANCEL
        // =====================================================

        Long sessionId =
                conversationSessions.get(
                        websocketId
                );


        if (
                sessionId != null
        ) {

            try {

                conversationInterviewService
                        .cancelCurrentProcess(
                                sessionId
                        );

            } catch (Exception e) {

                System.err.println(
                        "⚠️ SERVICE CANCEL ERROR: "
                                + e.getMessage()
                );
            }
        }


        // =====================================================
        // RESET AI STATE
        // =====================================================

        AtomicBoolean speaking =
                aiSpeaking.get(
                        websocketId
                );


        if (
                speaking != null
        ) {

            speaking.set(false);
        }


        // =====================================================
        // RESET ANSWER STATE
        // =====================================================

        AtomicBoolean processing =
                answerProcessing.get(
                        websocketId
                );


        if (
                processing != null
        ) {

            processing.set(false);
        }


        // =====================================================
        // NOTIFY CLIENT
        // =====================================================

        if (
                notifyClient &&
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
    // END INTERVIEW
    // =========================================================

    private void handleEndInterview(
            WebSocketSession websocket
    ) {

        String websocketId =
                websocket.getId();


        Long sessionId =
                conversationSessions.get(
                        websocketId
                );


        if (
                sessionId == null
        ) {

            sendError(
                    websocket,
                    "Conversation session not found"
            );

            return;
        }


        // =====================================================
        // CANCEL AI
        // =====================================================

        cancelAI(
                websocket,
                false
        );


        // =====================================================
        // CLEAR AUDIO
        // =====================================================

        clearAudioBuffer(
                websocketId
        );


        try {

            var response =
                    conversationInterviewService
                            .endInterview(
                                    sessionId
                            );


            Map<String, Object>
                    result =
                    new HashMap<>();


            result.put(
                    "type",
                    "INTERVIEW_COMPLETED"
            );


            result.put(
                    "overallScore",
                    response == null
                            ? null
                            : response.getOverallScore()
            );


            result.put(
                    "finalFeedback",
                    response == null
                            ? null
                            : response.getFinalFeedback()
            );


            result.put(
                    "totalMessages",
                    response == null
                            ? null
                            : response.getTotalMessages()
            );


            sendJson(
                    websocket,
                    result
            );


        } catch (Exception e) {

            System.err.println(
                    "❌ END INTERVIEW ERROR: "
                            + e.getMessage()
            );


            sendError(
                    websocket,
                    e.getMessage()
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
                node == null ||
                        node.isNull()
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
                    value == null ||
                            value.isBlank()
            ) {

                return null;
            }


            return Long.parseLong(
                    value.trim()
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
                session == null ||
                        !session.isOpen()
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


        } catch (Exception e) {

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

        Map<String, Object>
                error =
                new HashMap<>();


        error.put(
                "type",
                "ERROR"
        );


        error.put(
                "message",
                message == null ||
                        message.isBlank()
                        ? "Unknown error"
                        : message
        );


        sendJson(
                session,
                error
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

        if (
                session == null
        ) {

            return;
        }


        String websocketId =
                session.getId();


        System.out.println(
                "========================================"
        );

        System.out.println(
                "🔌 CONVERSATION WEBSOCKET CLOSED"
        );

        System.out.println(
                "ID: "
                        + websocketId
        );

        System.out.println(
                "STATUS: "
                        + status
        );

        System.out.println(
                "========================================"
        );


        // =====================================================
        // CANCEL AI
        // =====================================================

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
                        "⚠️ AI DISPOSE ERROR: "
                                + e.getMessage()
                );
            }
        }


        // =====================================================
        // CANCEL SERVICE
        // =====================================================

        Long sessionId =
                conversationSessions.get(
                        websocketId
                );


        if (
                sessionId != null
        ) {

            try {

                conversationInterviewService
                        .cancelCurrentProcess(
                                sessionId
                        );

            } catch (Exception e) {

                System.err.println(
                        "⚠️ SERVICE CANCEL ERROR: "
                                + e.getMessage()
                );
            }
        }


        // =====================================================
        // REMOVE AUDIO
        // =====================================================

        ByteArrayOutputStream buffer =
                audioBuffers.remove(
                        websocketId
                );


        if (
                buffer != null
        ) {

            try {

                buffer.close();

            } catch (IOException e) {

                // ignore
            }
        }


        // =====================================================
        // REMOVE MAPS
        // =====================================================

        conversationSessions.remove(
                websocketId
        );


        websocketSessions.remove(
                websocketId
        );


        activeAiProcesses.remove(
                websocketId
        );


        aiSpeaking.remove(
                websocketId
        );


        answerProcessing.remove(
                websocketId
        );


        audioBuffers.remove(
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
                "❌ CONVERSATION WS TRANSPORT ERROR: "
                        +
                        (
                                exception == null
                                        ? "Unknown"
                                        : exception.getMessage()
                        )
        );


        if (
                session == null
        ) {

            return;
        }


        try {

            cancelAI(
                    session,
                    false
            );


            if (
                    session.isOpen()
            ) {

                session.close(
                        CloseStatus.SERVER_ERROR
                );
            }


        } catch (IOException e) {

            System.err.println(
                    "❌ WS CLOSE ERROR: "
                            + e.getMessage()
            );
        }
    }
}