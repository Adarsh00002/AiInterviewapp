package com.example.MyFirstApp.service.GroqService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class GroqSTTService {

    private final WebClient webClient;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Value("${GROQ_APIKEY}")
    private String apiKey;

    // =========================================================
    // GROQ STT
    // =========================================================

    private static final String GROQ_STT_URL =
            "https://api.groq.com/openai/v1/audio/transcriptions";

    // Fast + multilingual
    private static final String MODEL =
            "whisper-large-v3-turbo";

    // =========================================================
    // AUDIO CONFIG
    //
    // Frontend sends:
    //
    // PCM 16-bit
    // Mono
    // 16000 Hz
    //
    // 16000 * 2 = 32000 bytes/sec
    // =========================================================

    private static final int SAMPLE_RATE = 16000;

    private static final int CHANNELS = 1;

    private static final int BITS_PER_SAMPLE = 16;

    // =========================================================
    // SESSION AUDIO BUFFERS
    // =========================================================

    private final Map<String, ByteArrayOutputStream>
            audioBuffers =
            new ConcurrentHashMap<>();

    // =========================================================
    // TRANSCRIPT CALLBACKS
    // =========================================================

    private final Map<String, BiConsumer<String, String>>
            transcriptCallbacks =
            new ConcurrentHashMap<>();

    // =========================================================
    // PROCESSING LOCK
    // =========================================================

    private final Map<String, Boolean>
            processing =
            new ConcurrentHashMap<>();

    // =========================================================
    // CONNECT
    //
    // Groq realtime WebSocket nahi hai.
    //
    // Isliye yahan sirf session state initialize hota hai.
    // =========================================================

    public synchronized void connect(
            String sessionId,
            BiConsumer<String, String> transcriptCallback) {

        if (
                sessionId == null
                        || sessionId.isBlank()
        ) {
            return;
        }

        transcriptCallbacks.put(
                sessionId,
                transcriptCallback
        );

        audioBuffers.computeIfAbsent(
                sessionId,
                id -> new ByteArrayOutputStream()
        );

        processing.put(
                sessionId,
                false
        );

        System.out.println(
                "======================================"
        );

        System.out.println(
                "✅ GROQ STT SESSION CREATED"
        );

        System.out.println(
                "SESSION: " + sessionId
        );

        System.out.println(
                "MODEL: " + MODEL
        );

        System.out.println(
                "AUDIO: PCM 16-bit / MONO / 16kHz"
        );

        System.out.println(
                "======================================"
        );
    }

    // =========================================================
    // SEND AUDIO
    //
    // Frontend se PCM chunks yahan aayenge.
    // =========================================================

    public void sendAudio(
            String sessionId,
            byte[] audioBytes) {

        if (
                sessionId == null
                        || audioBytes == null
                        || audioBytes.length == 0
        ) {
            return;
        }

        ByteArrayOutputStream buffer =
                audioBuffers.computeIfAbsent(
                        sessionId,
                        id -> new ByteArrayOutputStream()
                );

        synchronized (buffer) {

            try {

                buffer.write(
                        audioBytes
                );

            } catch (IOException e) {

                System.err.println(
                        "❌ PCM BUFFER WRITE ERROR: "
                                + e.getMessage()
                );
            }
        }

        System.out.println(
                "🎤 GROQ STT PCM BUFFERED: "
                        + audioBytes.length
                        + " bytes"
        );
    }

    // =========================================================
    // COMMIT AUDIO
    //
    // END_AUDIO par:
    //
    // PCM → WAV
    // WAV → Groq Whisper
    // Whisper → transcript
    // =========================================================

    public void commitAudio(
            String sessionId) {

        if (
                sessionId == null
                        || sessionId.isBlank()
        ) {
            return;
        }

        Boolean isProcessing =
                processing.get(sessionId);

        if (
                Boolean.TRUE.equals(
                        isProcessing
                )
        ) {

            System.out.println(
                    "⚠️ GROQ STT ALREADY PROCESSING: "
                            + sessionId
            );

            return;
        }

        ByteArrayOutputStream buffer =
                audioBuffers.get(sessionId);

        if (
                buffer == null
        ) {

            sendTooShort(
                    sessionId
            );

            return;
        }

        byte[] pcmBytes;

        synchronized (buffer) {

            pcmBytes =
                    buffer.toByteArray();

            // ===============================================
            // RESET BUFFER IMMEDIATELY
            //
            // New answer ka audio parallel accumulate ho sakta
            // hai once processing starts.
            // ===============================================

            buffer.reset();
        }

        if (
                pcmBytes.length == 0
        ) {

            sendTooShort(
                    sessionId
            );

            return;
        }

        double durationSeconds =
                pcmBytes.length /
                        (double)
                                (SAMPLE_RATE * 2);

        System.out.println(
                "======================================"
        );

        System.out.println(
                "🛑 GROQ STT COMMIT"
        );

        System.out.println(
                "SESSION: " + sessionId
        );

        System.out.println(
                "PCM BYTES: "
                        + pcmBytes.length
        );

        System.out.println(
                "AUDIO DURATION: "
                        + durationSeconds
                        + " sec"
        );

        System.out.println(
                "======================================"
        );

        // Too short check
        if (
                durationSeconds < 0.3
        ) {

            sendTooShort(
                    sessionId
            );

            return;
        }

        processing.put(
                sessionId,
                true
        );

        byte[] wavBytes;

        try {

            wavBytes =
                    pcmToWav(
                            pcmBytes
                    );

        } catch (Exception e) {

            processing.put(
                    sessionId,
                    false
            );

            System.err.println(
                    "❌ PCM → WAV ERROR: "
                            + e.getMessage()
            );

            sendError(
                    sessionId,
                    e
            );

            return;
        }

        transcribe(
                sessionId,
                wavBytes
        );
    }

    // =========================================================
    // TRANSCRIBE
    // =========================================================

    private void transcribe(
            String sessionId,
            byte[] wavBytes) {

        ByteArrayResource audioResource =
                new ByteArrayResource(
                        wavBytes
                ) {

                    @Override
                    public String getFilename() {

                        return "interview.wav";
                    }
                };

        MultiValueMap<String, Object> multipart =
                new LinkedMultiValueMap<>();

        multipart.add(
                "file",
                audioResource
        );

        multipart.add(
                "model",
                MODEL
        );

        // English interview
        //
        // language specify karne se latency/accuracy improve
        // ho sakti hai.
        //
        // Agar Hindi + English mixed expected hai,
        // ise remove kar sakte ho.
        multipart.add(
                "language",
                "en"
        );

        multipart.add(
                "response_format",
                "json"
        );

        multipart.add(
                "temperature",
                "0"
        );

        System.out.println(
                "======================================"
        );

        System.out.println(
                "🤖 GROQ WHISPER TRANSCRIPTION STARTED"
        );

        System.out.println(
                "SESSION: " + sessionId
        );

        System.out.println(
                "MODEL: " + MODEL
        );

        System.out.println(
                "WAV BYTES: " + wavBytes.length
        );

        System.out.println(
                "======================================"
        );

        webClient.post()

                .uri(
                        GROQ_STT_URL
                )

                .header(
                        "Authorization",
                        "Bearer " + apiKey
                )

                .contentType(
                        MediaType.MULTIPART_FORM_DATA
                )

                .body(
                        BodyInserters
                                .fromMultipartData(
                                        multipart
                                )
                )

                .retrieve()

                // =================================================
                // GROQ ERROR BODY
                // =================================================

                .onStatus(
                        status ->
                                status.isError(),

                        response ->
                                response
                                        .bodyToMono(
                                                String.class
                                        )
                                        .flatMap(
                                                errorBody ->
                                                        Mono.error(
                                                                new RuntimeException(
                                                                        "Groq STT error: "
                                                                                + errorBody
                                                                )
                                                        )
                                        )
                )

                .bodyToMono(
                        String.class
                )

                .subscribe(

                        json -> {

                            processing.put(
                                    sessionId,
                                    false
                            );

                            handleGroqResponse(
                                    sessionId,
                                    json
                            );
                        },

                        error -> {

                            processing.put(
                                    sessionId,
                                    false
                            );

                            System.err.println(
                                    "======================================"
                            );

                            System.err.println(
                                    "❌ GROQ STT ERROR"
                            );

                            System.err.println(
                                    "SESSION: "
                                            + sessionId
                            );

                            System.err.println(
                                    "MESSAGE: "
                                            + error.getMessage()
                            );

                            System.err.println(
                                    "======================================"
                            );

                            sendError(
                                    sessionId,
                                    error
                            );
                        }
                );
    }

    // =========================================================
    // GROQ RESPONSE
    // =========================================================

    private void handleGroqResponse(
            String sessionId,
            String json) {

        try {

            System.out.println(
                    "📩 GROQ STT RESPONSE:"
            );

            System.out.println(
                    json
            );

            JsonNode root =
                    objectMapper.readTree(
                            json
                    );

            String transcript =
                    root.path(
                                    "text"
                            )
                            .asText(
                                    ""
                            )
                            .trim();

            if (
                    transcript.isBlank()
            ) {

                System.err.println(
                        "⚠️ GROQ RETURNED EMPTY TRANSCRIPT"
                );

                BiConsumer<String, String>
                        callback =
                        transcriptCallbacks.get(
                                sessionId
                        );

                if (
                        callback != null
                ) {

                    callback.accept(
                            "empty",
                            ""
                    );
                }

                return;
            }

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "✅ GROQ COMMITTED TRANSCRIPT"
            );

            System.out.println(
                    "SESSION: " + sessionId
            );

            System.out.println(
                    "TEXT: " + transcript
            );

            System.out.println(
                    "======================================"
            );

            BiConsumer<String, String>
                    callback =
                    transcriptCallbacks.get(
                            sessionId
                    );

            if (
                    callback != null
            ) {

                callback.accept(
                        "committed",
                        transcript
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "❌ GROQ STT JSON PARSE ERROR: "
                            + e.getMessage()
            );

            sendError(
                    sessionId,
                    e
            );
        }
    }

    // =========================================================
    // TOO SHORT
    // =========================================================

    private void sendTooShort(
            String sessionId) {

        BiConsumer<String, String>
                callback =
                transcriptCallbacks.get(
                        sessionId
                );

        if (
                callback != null
        ) {

            callback.accept(
                    "too_short",
                    ""
            );
        }
    }

    // =========================================================
    // ERROR CALLBACK
    // =========================================================

    private void sendError(
            String sessionId,
            Throwable error) {

        BiConsumer<String, String>
                callback =
                transcriptCallbacks.get(
                        sessionId
                );

        if (
                callback != null
        ) {

            callback.accept(
                    "error",
                    error.getMessage()
            );
        }
    }

    // =========================================================
    // PCM → WAV
    // =========================================================

    private byte[] pcmToWav(
            byte[] pcmData) {

        int byteRate =
                SAMPLE_RATE
                        * CHANNELS
                        * BITS_PER_SAMPLE
                        / 8;

        int blockAlign =
                CHANNELS
                        * BITS_PER_SAMPLE
                        / 8;

        int dataLength =
                pcmData.length;

        int fileLength =
                36 + dataLength;

        ByteBuffer buffer =
                ByteBuffer
                        .allocate(
                                44 + dataLength
                        )
                        .order(
                                ByteOrder.LITTLE_ENDIAN
                        );

        // =====================================================
        // RIFF
        // =====================================================

        buffer.put(
                new byte[]{
                        'R',
                        'I',
                        'F',
                        'F'
                }
        );

        buffer.putInt(
                fileLength
        );

        buffer.put(
                new byte[]{
                        'W',
                        'A',
                        'V',
                        'E'
                }
        );

        // =====================================================
        // fmt
        // =====================================================

        buffer.put(
                new byte[]{
                        'f',
                        'm',
                        't',
                        ' '
                }
        );

        buffer.putInt(
                16
        );

        // PCM format
        buffer.putShort(
                (short) 1
        );

        buffer.putShort(
                (short) CHANNELS
        );

        buffer.putInt(
                SAMPLE_RATE
        );

        buffer.putInt(
                byteRate
        );

        buffer.putShort(
                (short) blockAlign
        );

        buffer.putShort(
                (short) BITS_PER_SAMPLE
        );

        // =====================================================
        // data
        // =====================================================

        buffer.put(
                new byte[]{
                        'd',
                        'a',
                        't',
                        'a'
                }
        );

        buffer.putInt(
                dataLength
        );

        buffer.put(
                pcmData
        );

        return buffer.array();
    }

    // =========================================================
    // CLOSE
    // =========================================================

    public synchronized void close(
            String sessionId) {

        System.out.println(
                "======================================"
        );

        System.out.println(
                "🗑️ CLOSING GROQ STT SESSION"
        );

        System.out.println(
                "SESSION: " + sessionId
        );

        System.out.println(
                "======================================"
        );

        audioBuffers.remove(
                sessionId
        );

        transcriptCallbacks.remove(
                sessionId
        );

        processing.remove(
                sessionId
        );
    }
}