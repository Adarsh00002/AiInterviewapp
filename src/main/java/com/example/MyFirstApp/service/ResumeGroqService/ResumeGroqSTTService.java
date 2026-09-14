package com.example.MyFirstApp.service.ResumeGroqService;


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
public class ResumeGroqSTTService {

    private final WebClient webClient;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Value("${GROQ_APIKEY}")
    private String apiKey;

    private static final String STT_URL =
            "https://api.groq.com/openai/v1/audio/transcriptions";

    private static final String MODEL =
            "whisper-large-v3-turbo";

    private static final int SAMPLE_RATE = 16000;

    private static final int CHANNELS = 1;

    private static final int BITS_PER_SAMPLE = 16;


    private final Map<String, ByteArrayOutputStream>
            audioBuffers =
            new ConcurrentHashMap<>();

    private final Map<String, BiConsumer<String, String>>
            transcriptCallbacks =
            new ConcurrentHashMap<>();

    private final Map<String, Boolean>
            processing =
            new ConcurrentHashMap<>();


    // =========================================================
    // CONNECT
    // =========================================================

    public synchronized void connect(
            String websocketId,
            BiConsumer<String, String> callback
    ) {

        if (
                websocketId == null
                        || websocketId.isBlank()
        ) {
            return;
        }

        transcriptCallbacks.put(
                websocketId,
                callback
        );

        audioBuffers.computeIfAbsent(
                websocketId,
                id -> new ByteArrayOutputStream()
        );

        processing.put(
                websocketId,
                false
        );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "🎤 RESUME STT SESSION CONNECTED"
        );

        System.out.println(
                "WEBSOCKET: " + websocketId
        );

        System.out.println(
                "========================================"
        );
    }


    // =========================================================
    // PCM AUDIO
    // =========================================================

    public void sendAudio(
            String websocketId,
            byte[] audioBytes
    ) {

        if (
                websocketId == null
                        || audioBytes == null
                        || audioBytes.length == 0
        ) {
            return;
        }

        ByteArrayOutputStream buffer =
                audioBuffers.computeIfAbsent(
                        websocketId,
                        id -> new ByteArrayOutputStream()
                );

        synchronized (buffer) {

            try {

                buffer.write(
                        audioBytes
                );

            } catch (IOException e) {

                sendError(
                        websocketId,
                        e
                );
            }
        }
    }


    // =========================================================
    // END USER AUDIO
    // =========================================================

    public void commitAudio(
            String websocketId
    ) {

        if (
                websocketId == null
                        || websocketId.isBlank()
        ) {
            return;
        }

        if (
                Boolean.TRUE.equals(
                        processing.get(websocketId)
                )
        ) {

            return;
        }

        ByteArrayOutputStream buffer =
                audioBuffers.get(
                        websocketId
                );

        if (buffer == null) {

            sendTooShort(
                    websocketId
            );

            return;
        }

        byte[] pcmBytes;

        synchronized (buffer) {

            pcmBytes =
                    buffer.toByteArray();

            buffer.reset();
        }

        if (
                pcmBytes.length == 0
        ) {

            sendTooShort(
                    websocketId
            );

            return;
        }

        double duration =
                pcmBytes.length /
                        (double)
                                (SAMPLE_RATE * 2);

        System.out.println(
                "========================================"
        );

        System.out.println(
                "🎤 RESUME STT COMMIT"
        );

        System.out.println(
                "DURATION: "
                        + duration
                        + " sec"
        );

        System.out.println(
                "========================================"
        );

        if (duration < 0.3) {

            sendTooShort(
                    websocketId
            );

            return;
        }

        processing.put(
                websocketId,
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
                    websocketId,
                    false
            );

            sendError(
                    websocketId,
                    e
            );

            return;
        }

        transcribe(
                websocketId,
                wavBytes
        );
    }


    // =========================================================
    // TRANSCRIBE
    // =========================================================

    private void transcribe(
            String websocketId,
            byte[] wavBytes
    ) {

        ByteArrayResource audioResource =
                new ByteArrayResource(
                        wavBytes
                ) {

                    @Override
                    public String getFilename() {

                        return "resume-interview.wav";
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

        multipart.add(
                "response_format",
                "json"
        );

        multipart.add(
                "temperature",
                "0"
        );

        webClient
                .post()
                .uri(STT_URL)
                .header(
                        "Authorization",
                        "Bearer " + apiKey
                )
                .contentType(
                        MediaType.MULTIPART_FORM_DATA
                )
                .body(
                        BodyInserters.fromMultipartData(
                                multipart
                        )
                )
                .retrieve()
                .onStatus(
                        status ->
                                status.isError(),
                        response ->
                                response
                                        .bodyToMono(
                                                String.class
                                        )
                                        .flatMap(
                                                body ->
                                                        Mono.error(
                                                                new RuntimeException(
                                                                        "Resume STT error: "
                                                                                + body
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
                                    websocketId,
                                    false
                            );

                            handleResponse(
                                    websocketId,
                                    json
                            );
                        },

                        error -> {

                            processing.put(
                                    websocketId,
                                    false
                            );

                            sendError(
                                    websocketId,
                                    error
                            );
                        }
                );
    }


    // =========================================================
    // HANDLE RESPONSE
    // =========================================================

    private void handleResponse(
            String websocketId,
            String json
    ) {

        try {

            JsonNode root =
                    objectMapper.readTree(
                            json
                    );

            String text =
                    root.path("text")
                            .asText("")
                            .trim();

            if (
                    text.isBlank()
            ) {

                sendTooShort(
                        websocketId
                );

                return;
            }

            BiConsumer<String, String>
                    callback =
                    transcriptCallbacks.get(
                            websocketId
                    );

            if (callback != null) {

                callback.accept(
                        "committed",
                        text
                );
            }

        } catch (Exception e) {

            sendError(
                    websocketId,
                    e
            );
        }
    }


    // =========================================================
    // TOO SHORT
    // =========================================================

    private void sendTooShort(
            String websocketId
    ) {

        BiConsumer<String, String>
                callback =
                transcriptCallbacks.get(
                        websocketId
                );

        if (callback != null) {

            callback.accept(
                    "too_short",
                    ""
            );
        }
    }


    // =========================================================
    // ERROR
    // =========================================================

    private void sendError(
            String websocketId,
            Throwable error
    ) {

        BiConsumer<String, String>
                callback =
                transcriptCallbacks.get(
                        websocketId
                );

        if (callback != null) {

            callback.accept(
                    "error",
                    error == null
                            ? "Unknown STT error"
                            : error.getMessage()
            );
        }
    }


    // =========================================================
    // PCM → WAV
    // =========================================================

    private byte[] pcmToWav(
            byte[] pcmData
    ) {

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

        buffer.put(
                new byte[]{
                        'R', 'I', 'F', 'F'
                }
        );

        buffer.putInt(
                fileLength
        );

        buffer.put(
                new byte[]{
                        'W', 'A', 'V', 'E'
                }
        );

        buffer.put(
                new byte[]{
                        'f', 'm', 't', ' '
                }
        );

        buffer.putInt(
                16
        );

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

        buffer.put(
                new byte[]{
                        'd', 'a', 't', 'a'
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
            String websocketId
    ) {

        audioBuffers.remove(
                websocketId
        );

        transcriptCallbacks.remove(
                websocketId
        );

        processing.remove(
                websocketId
        );
    }
}