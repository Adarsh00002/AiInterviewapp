package com.example.MyFirstApp.service.HRandCommunication;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ConversationGroqTTService {


    private final WebClient webClient;


    // =========================================================
    // GROQ API KEY
    // =========================================================

    @Value("${GROQ_APIKEY}")
    private String apiKey;


    // =========================================================
    // AUDIO UPLOAD PATH
    // =========================================================

    @Value("${audio.upload.path}")
    private String uploadPath;


    // =========================================================
    // SERVER URL
    // =========================================================

    @Value("${app.base-url:http://192.168.43.75:8080}")
    private String baseUrl;


    // =========================================================
    // GROQ TTS
    // =========================================================

    private static final String TTS_URL =
            "https://api.groq.com/openai/v1/audio/speech";


    private static final String MODEL =
            "canopylabs/orpheus-v1-english";


    private static final String VOICE =
            "hannah";


    // =========================================================
    // GENERATE COMPLETE AUDIO
    // =========================================================
    //
    // FULL TEXT
    //      ↓
    // ONE GROQ REQUEST
    //      ↓
    // WAV
    //      ↓
    // MP3
    //      ↓
    // audioUrl
    //
    // =========================================================

    public Disposable generateSpeechChunks(

            String text,

            BiConsumer<String, String> onAudioReady,

            Runnable onComplete,

            Consumer<Throwable> onError

    ) {


        AtomicBoolean cancelled =
                new AtomicBoolean(false);


        AtomicReference<Disposable> activeDisposable =
                new AtomicReference<>();


        // =====================================================
        // PUBLIC DISPOSABLE
        // =====================================================

        Disposable publicDisposable =
                new Disposable() {

                    @Override
                    public void dispose() {

                        if (
                                !cancelled.compareAndSet(
                                        false,
                                        true
                                )
                        ) {

                            return;
                        }


                        System.out.println(
                                "🛑 CONVERSATION GROQ TTS CANCELLED"
                        );


                        Disposable current =
                                activeDisposable.get();


                        if (
                                current != null
                        ) {

                            current.dispose();
                        }
                    }


                    @Override
                    public boolean isDisposed() {

                        return cancelled.get();
                    }
                };


        // =====================================================
        // VALIDATE TEXT
        // =====================================================

        if (
                text == null ||
                        text.isBlank()
        ) {

            RuntimeException error =
                    new IllegalArgumentException(
                            "TTS text cannot be empty"
                    );


            if (
                    onError != null
            ) {

                onError.accept(
                        error
                );
            }


            return publicDisposable;
        }


        // =====================================================
        // CLEAN TEXT
        // =====================================================

        String cleanText =
                cleanText(
                        text
                );


        // =====================================================
        // CREATE DIRECTORY
        // =====================================================

        try {

            Files.createDirectories(
                    Paths.get(
                            uploadPath
                    )
            );

        } catch (Exception e) {

            if (
                    onError != null
            ) {

                onError.accept(
                        e
                );
            }

            return publicDisposable;
        }


        System.out.println(
                "========================================"
        );

        System.out.println(
                "🔊 CONVERSATION GROQ TTS STARTED"
        );

        System.out.println(
                "MODEL: "
                        + MODEL
        );

        System.out.println(
                "VOICE: "
                        + VOICE
        );

        System.out.println(
                "MODE: ONE COMPLETE AUDIO"
        );

        System.out.println(
                "TEXT LENGTH: "
                        + cleanText.length()
        );

        System.out.println(
                "========================================"
        );


        // =====================================================
        // SINGLE TTS REQUEST
        // =====================================================

        Disposable requestDisposable =
                generateAudio(
                        cleanText
                )
                        .subscribeOn(
                                Schedulers.boundedElastic()
                        )
                        .subscribe(

                                // =================================
                                // SUCCESS
                                // =================================

                                audioUrl -> {

                                    if (
                                            cancelled.get()
                                    ) {

                                        System.out.println(
                                                "🚫 IGNORING TTS RESULT - CANCELLED"
                                        );

                                        return;
                                    }


                                    if (
                                            audioUrl == null ||
                                                    audioUrl.isBlank()
                                    ) {

                                        RuntimeException error =
                                                new RuntimeException(
                                                        "Groq TTS returned empty audio URL"
                                                );


                                        if (
                                                onError != null
                                        ) {

                                            onError.accept(
                                                    error
                                            );
                                        }

                                        return;
                                    }


                                    System.out.println(
                                            "========================================"
                                    );

                                    System.out.println(
                                            "✅ CONVERSATION FULL AUDIO READY"
                                    );

                                    System.out.println(
                                            "AUDIO URL: "
                                                    + audioUrl
                                    );

                                    System.out.println(
                                            "TEXT: "
                                                    + cleanText
                                    );

                                    System.out.println(
                                            "========================================"
                                    );


                                    // =================================
                                    // AI AUDIO CALLBACK
                                    // =================================

                                    if (
                                            onAudioReady != null
                                    ) {

                                        try {

                                            onAudioReady.accept(
                                                    cleanText,
                                                    audioUrl
                                            );

                                        } catch (
                                                Exception callbackError
                                        ) {

                                            System.err.println(
                                                    "❌ CONVERSATION TTS CALLBACK ERROR: "
                                                            + callbackError.getMessage()
                                            );
                                        }
                                    }


                                    // =================================
                                    // COMPLETE
                                    // =================================

                                    if (
                                            !cancelled.get() &&
                                                    onComplete != null
                                    ) {

                                        try {

                                            onComplete.run();

                                        } catch (
                                                Exception callbackError
                                        ) {

                                            System.err.println(
                                                    "❌ CONVERSATION TTS COMPLETE CALLBACK ERROR: "
                                                            + callbackError.getMessage()
                                            );
                                        }
                                    }

                                },


                                // =================================
                                // ERROR
                                // =================================

                                error -> {

                                    if (
                                            cancelled.get()
                                    ) {

                                        return;
                                    }


                                    System.err.println(
                                            "========================================"
                                    );

                                    System.err.println(
                                            "❌ CONVERSATION GROQ TTS ERROR"
                                    );

                                    System.err.println(
                                            error == null
                                                    ? "Unknown TTS error"
                                                    : error.getMessage()
                                    );

                                    System.err.println(
                                            "========================================"
                                    );


                                    if (
                                            onError != null
                                    ) {

                                        try {

                                            onError.accept(
                                                    error
                                            );

                                        } catch (
                                                Exception callbackError
                                        ) {

                                            System.err.println(
                                                    "❌ CONVERSATION TTS ERROR CALLBACK ERROR: "
                                                            + callbackError.getMessage()
                                            );
                                        }
                                    }
                                }
                        );


        activeDisposable.set(
                requestDisposable
        );


        // =====================================================
        // RACE CONDITION
        // =====================================================

        if (
                cancelled.get()
        ) {

            requestDisposable.dispose();
        }


        return publicDisposable;
    }


    // =========================================================
    // GENERATE AUDIO
    // =========================================================

    private Mono<String> generateAudio(
            String text
    ) {

        return webClient
                .post()
                .uri(
                        TTS_URL
                )

                .header(
                        "Authorization",
                        "Bearer " + apiKey
                )

                .contentType(
                        MediaType.APPLICATION_JSON
                )

                .accept(
                        MediaType.parseMediaType(
                                "audio/wav"
                        )
                )

                .bodyValue(
                        new GroqTTSRequest(
                                MODEL,
                                text,
                                VOICE,
                                "wav"
                        )
                )

                .retrieve()

                // =================================================
                // GROQ ERROR
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
                                                body ->
                                                        Mono.error(
                                                                new RuntimeException(
                                                                        "Conversation TTS HTTP "
                                                                                + response.statusCode()
                                                                                + ": "
                                                                                + body
                                                                )
                                                        )
                                        )
                )

                // =================================================
                // AUDIO BYTES
                // =================================================

                .bodyToMono(
                        byte[].class
                )

                // =================================================
                // SAVE WAV + CONVERT MP3
                // =================================================

                .map(
                        this::saveAudio
                );
    }


    // =========================================================
    // SAVE WAV → MP3
    // =========================================================

    private String saveAudio(
            byte[] bytes
    ) {

        try {

            if (
                    bytes == null ||
                            bytes.length == 0
            ) {

                throw new RuntimeException(
                        "Empty audio returned from Groq"
                );
            }


            // =================================================
            // DIRECTORY
            // =================================================

            Path directory =
                    Paths.get(
                            uploadPath
                    );


            Files.createDirectories(
                    directory
            );


            // =================================================
            // UNIQUE FILE NAME
            // =================================================

            String id =
                    UUID.randomUUID()
                            .toString();


            Path wavPath =
                    directory.resolve(
                            "conversation_"
                                    + id
                                    + ".wav"
                    );


            Path mp3Path =
                    directory.resolve(
                            "conversation_"
                                    + id
                                    + ".mp3"
                    );


            // =================================================
            // SAVE WAV
            // =================================================

            Files.write(
                    wavPath,
                    bytes
            );


            System.out.println(
                    "💾 CONVERSATION WAV SAVED:"
            );

            System.out.println(
                    wavPath.toAbsolutePath()
            );


            // =================================================
            // WAV → MP3
            // =================================================

            System.out.println(
                    "🎵 CONVERSATION FFMPEG WAV -> MP3"
            );


            Process process =
                    new ProcessBuilder(
                            "ffmpeg",
                            "-y",
                            "-hide_banner",
                            "-loglevel",
                            "error",

                            "-i",

                            wavPath
                                    .toAbsolutePath()
                                    .toString(),

                            "-codec:a",
                            "libmp3lame",

                            "-b:a",
                            "128k",

                            mp3Path
                                    .toAbsolutePath()
                                    .toString()
                    )
                            .redirectErrorStream(
                                    true
                            )
                            .start();


            // =================================================
            // READ FFMPEG OUTPUT
            // =================================================

            StringBuilder ffmpegOutput =
                    new StringBuilder();


            try (
                    java.io.BufferedReader reader =
                            new java.io.BufferedReader(
                                    new java.io.InputStreamReader(
                                            process.getInputStream()
                                    )
                            )
            ) {

                String line;

                while (
                        (line =
                                reader.readLine()) != null
                ) {

                    ffmpegOutput
                            .append(line)
                            .append(
                                    System.lineSeparator()
                            );
                }
            }


            // =================================================
            // WAIT
            // =================================================

            int exitCode =
                    process.waitFor();


            // =================================================
            // CHECK FFMPEG
            // =================================================

            if (
                    exitCode != 0
            ) {

                throw new RuntimeException(
                        "FFmpeg conversion failed. Exit code="
                                + exitCode
                                + "\n"
                                + ffmpegOutput
                );
            }


            // =================================================
            // VERIFY MP3
            // =================================================

            if (
                    !Files.exists(
                            mp3Path
                    )
            ) {

                throw new RuntimeException(
                        "MP3 file was not created"
                );
            }


            long mp3Size =
                    Files.size(
                            mp3Path
                    );


            if (
                    mp3Size <= 0
            ) {

                throw new RuntimeException(
                        "Generated MP3 file is empty"
                );
            }


            System.out.println(
                    "✅ CONVERSATION MP3 GENERATED"
            );

            System.out.println(
                    "MP3 SIZE: "
                            + mp3Size
                            + " bytes"
            );


            // =================================================
            // DELETE WAV
            // =================================================

            try {

                Files.deleteIfExists(
                        wavPath
                );

                System.out.println(
                        "🗑️ CONVERSATION WAV DELETED"
                );

            } catch (
                    Exception e
            ) {

                System.err.println(
                        "⚠️ CONVERSATION WAV DELETE FAILED: "
                                + e.getMessage()
                );
            }


            // =================================================
            // FINAL AUDIO URL
            // =================================================

            String audioUrl =
                    baseUrl
                            + "/api/v1.0/audio/"
                            + mp3Path
                            .getFileName()
                            .toString();


            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "🔊 CONVERSATION MP3 URL:"
            );

            System.out.println(
                    audioUrl
            );

            System.out.println(
                    "========================================"
            );


            return audioUrl;

        } catch (
                Exception e
        ) {

            throw new RuntimeException(
                    "Unable to generate Conversation interview audio",
                    e
            );
        }
    }


    // =========================================================
    // CLEAN TEXT
    // =========================================================

    private String cleanText(
            String text
    ) {

        if (
                text == null
        ) {

            return "";
        }


        return text
                .replace(
                        "\n",
                        " "
                )
                .replace(
                        "\r",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }


    // =========================================================
    // TTS REQUEST DTO
    // =========================================================

    private record GroqTTSRequest(

            String model,

            String input,

            String voice,

            String response_format

    ) {
    }
}