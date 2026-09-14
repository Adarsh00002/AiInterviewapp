package com.example.MyFirstApp.service.HRandCommunication;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ConversationGroqSTTService {

    private final WebClient webClient;

    @Value("${GROQ_APIKEY}")
    private String apiKey;


    private static final String STT_URL =
            "https://api.groq.com/openai/v1/audio/transcriptions";


    private static final String MODEL =
            "whisper-large-v3-turbo";


    // =========================================================
    // RAW PCM -> WAV
    // =========================================================

    private byte[] pcmToWav(byte[] pcmData) throws IOException {

        if (pcmData == null || pcmData.length == 0) {
            throw new IllegalArgumentException(
                    "PCM audio is empty"
            );
        }

        int sampleRate = 16000;
        int channels = 1;
        int bitsPerSample = 16;

        int byteRate =
                sampleRate
                        * channels
                        * bitsPerSample
                        / 8;

        int blockAlign =
                channels
                        * bitsPerSample
                        / 8;

        int dataLength = pcmData.length;

        int totalLength =
                36 + dataLength;

        ByteArrayOutputStream output =
                new ByteArrayOutputStream(
                        totalLength
                );

        // RIFF
        output.write("RIFF".getBytes());

        writeIntLE(
                output,
                totalLength
        );

        // WAVE
        output.write("WAVE".getBytes());

        // fmt
        output.write("fmt ".getBytes());

        writeIntLE(
                output,
                16
        );

        // PCM format
        writeShortLE(
                output,
                (short) 1
        );

        // channels
        writeShortLE(
                output,
                (short) channels
        );

        // sample rate
        writeIntLE(
                output,
                sampleRate
        );

        // byte rate
        writeIntLE(
                output,
                byteRate
        );

        // block align
        writeShortLE(
                output,
                (short) blockAlign
        );

        // bits per sample
        writeShortLE(
                output,
                (short) bitsPerSample
        );

        // data
        output.write(
                "data".getBytes()
        );

        writeIntLE(
                output,
                dataLength
        );

        output.write(
                pcmData
        );

        return output.toByteArray();
    }


    // =========================================================
    // LITTLE ENDIAN HELPERS
    // =========================================================

    private void writeIntLE(
            ByteArrayOutputStream output,
            int value
    ) {

        output.write(
                value & 0xff
        );

        output.write(
                (value >> 8) & 0xff
        );

        output.write(
                (value >> 16) & 0xff
        );

        output.write(
                (value >> 24) & 0xff
        );
    }


    private void writeShortLE(
            ByteArrayOutputStream output,
            short value
    ) {

        output.write(
                value & 0xff
        );

        output.write(
                (value >> 8) & 0xff
        );
    }


    // =========================================================
    // TRANSCRIBE
    // =========================================================

    public String transcribe(
            byte[] pcmData
    ) {

        try {

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "🎤 CONVERSATION GROQ STT"
            );

            System.out.println(
                    "RAW PCM BYTES: "
                            + pcmData.length
            );


            // =================================================
            // PCM -> WAV
            // =================================================

            byte[] wavData =
                    pcmToWav(
                            pcmData
                    );


            System.out.println(
                    "✅ WAV CREATED"
            );

            System.out.println(
                    "WAV BYTES: "
                            + wavData.length
            );


            // =================================================
            // FILE RESOURCE
            // =================================================

            ByteArrayResource fileResource =
                    new ByteArrayResource(
                            wavData
                    ) {

                        @Override
                        public String getFilename() {
                            return "conversation.wav";
                        }
                    };


            // =================================================
            // MULTIPART FORM
            // =================================================

            MultiValueMap<String, Object> formData =
                    new LinkedMultiValueMap<>();


            formData.add(
                    "file",
                    fileResource
            );


            formData.add(
                    "model",
                    MODEL
            );


            formData.add(
                    "language",
                    "en"
            );


            formData.add(
                    "response_format",
                    "json"
            );


            formData.add(
                    "temperature",
                    "0"
            );


            // =================================================
            // GROQ REQUEST
            // =================================================

            String response =
                    webClient
                            .post()
                            .uri(
                                    STT_URL
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
                                                    formData
                                            )
                            )
                            .retrieve()
                            .onStatus(
                                    status ->
                                            status.isError(),

                                    clientResponse ->
                                            clientResponse
                                                    .bodyToMono(
                                                            String.class
                                                    )
                                                    .flatMap(
                                                            errorBody ->
                                                                    reactor.core.publisher.Mono.error(
                                                                            new RuntimeException(
                                                                                    "Groq STT HTTP "
                                                                                            + clientResponse.statusCode()
                                                                                            + ": "
                                                                                            + errorBody
                                                                            )
                                                                    )
                                                    )
                            )
                            .bodyToMono(
                                    String.class
                            )
                            .block();


            System.out.println(
                    "✅ GROQ STT RESPONSE:"
            );

            System.out.println(
                    response
            );

            System.out.println(
                    "========================================"
            );


            // =================================================
            // EXTRACT TEXT
            // =================================================

            if (
                    response == null ||
                            response.isBlank()
            ) {

                return "";
            }


            try {

                com.fasterxml.jackson.databind.JsonNode node =
                        new com.fasterxml.jackson.databind.ObjectMapper()
                                .readTree(
                                        response
                                );

                return node
                        .path("text")
                        .asText(
                                ""
                        );

            } catch (Exception jsonError) {

                System.out.println(
                        "⚠️ STT JSON PARSE ERROR: "
                                + jsonError.getMessage()
                );

                return response;
            }


        } catch (Exception error) {

            System.err.println(
                    "❌ CONVERSATION GROQ STT ERROR: "
                            + error.getMessage()
            );

            throw new RuntimeException(
                    error
            );
        }
    }
}