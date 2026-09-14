package com.example.MyFirstApp.service.ResumeGroqService;

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
public class ResumeGroqTTService {

    private final WebClient webClient;

    @Value("${GROQ_APIKEY}")
    private String apiKey;

    @Value("${audio.upload.path}")
    private String uploadPath;


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
    // GENERATE ONE COMPLETE AUDIO
    // =========================================================
    //
    // IMPORTANT:
    //
    // AI response ko split nahi karna.
    //
    // FULL TEXT
    //     ↓
    // ONE GROQ TTS REQUEST
    //     ↓
    // ONE WAV
    //     ↓
    // ONE MP3
    //     ↓
    // ONE audioUrl
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
                                "🛑 RESUME GROQ TTS CANCELLED"
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
        // VALIDATION
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
                "🔊 RESUME GROQ TTS STARTED"
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
                                            "✅ RESUME GROQ FULL AUDIO READY"
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
                                    // ONE AI_AUDIO CALLBACK
                                    // =================================

                                    if (
                                            onAudioReady != null
                                    ) {

                                        try {

                                            onAudioReady.accept(
                                                    cleanText,
                                                    audioUrl
                                            );

                                        } catch (Exception callbackError) {

                                            System.err.println(
                                                    "❌ RESUME TTS CALLBACK ERROR: "
                                                            + callbackError.getMessage()
                                            );
                                        }
                                    }


                                    // =================================
                                    // GENERATION COMPLETE
                                    // =================================

                                    if (
                                            !cancelled.get() &&
                                                    onComplete != null
                                    ) {

                                        try {

                                            onComplete.run();

                                        } catch (Exception callbackError) {

                                            System.err.println(
                                                    "❌ RESUME TTS COMPLETE CALLBACK ERROR: "
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
                                            "❌ RESUME GROQ TTS ERROR"
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

                                        } catch (Exception callbackError) {

                                            System.err.println(
                                                    "❌ RESUME TTS ERROR CALLBACK ERROR: "
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
    // SINGLE AUDIO REQUEST
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
                                                                        "Resume TTS HTTP "
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
                // SAVE AUDIO
                // =================================================

                .map(
                        this::saveAudio
                );
    }


    // =========================================================
    // SAVE AUDIO
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


            Path directory =
                    Paths.get(
                            uploadPath
                    );


            Files.createDirectories(
                    directory
            );


            String id =
                    UUID.randomUUID()
                            .toString();


            Path wavPath =
                    directory.resolve(
                            id + ".wav"
                    );


            Path mp3Path =
                    directory.resolve(
                            id + ".mp3"
                    );


            // =================================================
            // SAVE WAV
            // =================================================

            Files.write(
                    wavPath,
                    bytes
            );


            System.out.println(
                    "💾 RESUME WAV SAVED: "
                            + wavPath.toAbsolutePath()
            );


            // =================================================
            // WAV -> MP3
            // =================================================

            System.out.println(
                    "🎵 RESUME FFMPEG WAV -> MP3"
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
                            .start();


            int exitCode =
                    process.waitFor();


            if (
                    exitCode != 0
            ) {

                throw new RuntimeException(
                        "FFmpeg conversion failed. Exit code: "
                                + exitCode
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
                    "✅ RESUME MP3 GENERATED"
            );

            System.out.println(
                    "SIZE: "
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

            } catch (Exception e) {

                System.err.println(
                        "⚠️ RESUME WAV DELETE FAILED: "
                                + e.getMessage()
                );
            }


            // =================================================
            // AUDIO URL
            // =================================================

            String audioUrl =
                    "http://192.168.43.75:8080"
                            + "/api/v1.0/audio/"
                            + mp3Path.getFileName()
                            .toString();


            System.out.println(
                    "🔊 RESUME FULL AUDIO URL:"
            );

            System.out.println(
                    audioUrl
            );


            return audioUrl;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to generate Resume interview audio",
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
    // DTO
    // =========================================================

    private record GroqTTSRequest(

            String model,

            String input,

            String voice,

            String response_format

    ) {
    }
}