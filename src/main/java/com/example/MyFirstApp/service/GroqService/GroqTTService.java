package com.example.MyFirstApp.service.GroqService;

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
public class GroqTTService {

    // =========================================================
    // WEB CLIENT
    // =========================================================

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
    // GROQ TTS URL
    // =========================================================

    private static final String TTS_URL =
            "https://api.groq.com/openai/v1/audio/speech";


    // =========================================================
    // MODEL
    // =========================================================

    private static final String MODEL =
            "canopylabs/orpheus-v1-english";


    // =========================================================
    // VOICE
    // =========================================================

    private static final String VOICE =
            "hannah";


    // =========================================================
    // NORMAL SINGLE TTS
    // =========================================================

    public String generateSpeech(
            String text
    ) {

        try {

            if (
                    text == null ||
                            text.isBlank()
            ) {

                throw new IllegalArgumentException(
                        "TTS text cannot be empty"
                );
            }

            Path audioDirectory =
                    Paths.get(uploadPath);

            Files.createDirectories(
                    audioDirectory
            );

            String cleanText =
                    cleanText(text);

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "🔊 GROQ SINGLE TTS STARTED"
            );

            System.out.println(
                    "MODEL: " + MODEL
            );

            System.out.println(
                    "VOICE: " + VOICE
            );

            System.out.println(
                    "TEXT LENGTH: " + cleanText.length()
            );

            System.out.println(
                    "========================================"
            );

            return generateSingleAudio(
                    cleanText
            );

        } catch (Exception e) {

            System.err.println(
                    "❌ GROQ TTS GENERATE ERROR: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Groq TTS generation failed",
                    e
            );
        }
    }


    // =========================================================
    // OLD COMPATIBLE METHOD
    // =========================================================

    public Disposable generateSpeechChunks(

            String text,

            Consumer<String> onAudioReady,

            Consumer<Throwable> onError

    ) {

        return generateSpeechChunks(

                text,

                onAudioReady,

                () -> {

                    System.out.println(
                            "✅ GROQ TTS COMPLETED"
                    );
                },

                onError
        );
    }


    // =========================================================
    // OLD CALLBACK METHOD
    //
    // IMPORTANT:
    // Method name remains same so existing code does not break.
    //
    // BUT internally it now generates ONE COMPLETE AUDIO FILE.
    // =========================================================

    public Disposable generateSpeechChunks(

            String text,

            Consumer<String> onAudioReady,

            Runnable onComplete,

            Consumer<Throwable> onError

    ) {

        return generateSpeechChunksWithText(

                text,

                (
                        textChunk,
                        audioUrl
                ) -> {

                    if (
                            onAudioReady != null
                    ) {

                        onAudioReady.accept(
                                audioUrl
                        );
                    }
                },

                onComplete,

                onError
        );
    }


    // =========================================================
    // MAIN TTS METHOD
    //
    // IMPORTANT:
    //
    // Previously:
    //
    // full text
    //    ↓
    // chunk 1
    // chunk 2
    // chunk 3
    //    ↓
    // multiple MP3
    //
    // NOW:
    //
    // full text
    //    ↓
    // ONE TTS REQUEST
    //    ↓
    // ONE MP3
    //
    // This removes audio gaps between chunks.
    // =========================================================

    public Disposable generateSpeechChunksWithText(

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
                                cancelled.compareAndSet(
                                        false,
                                        true
                                )
                        ) {

                            System.out.println(
                                    "🛑 GROQ SINGLE TTS CANCELLED"
                            );

                            Disposable current =
                                    activeDisposable.get();

                            if (
                                    current != null
                            ) {

                                current.dispose();
                            }
                        }
                    }


                    @Override
                    public boolean isDisposed() {

                        return cancelled.get();
                    }
                };


        try {

            // =================================================
            // VALIDATION
            // =================================================

            if (
                    text == null ||
                            text.isBlank()
            ) {

                IllegalArgumentException error =
                        new IllegalArgumentException(
                                "TTS text cannot be empty"
                        );

                if (
                        onError != null
                ) {

                    onError.accept(error);
                }

                return publicDisposable;
            }


            // =================================================
            // DIRECTORY
            // =================================================

            Path audioDirectory =
                    Paths.get(uploadPath);

            Files.createDirectories(
                    audioDirectory
            );


            // =================================================
            // CLEAN TEXT
            // =================================================

            String cleanText =
                    cleanText(text);


            // =================================================
            // LOG
            // =================================================

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "🔊 GROQ TTS STARTED"
            );

            System.out.println(
                    "MODEL: " + MODEL
            );

            System.out.println(
                    "VOICE: " + VOICE
            );

            System.out.println(
                    "TEXT LENGTH: "
                            + cleanText.length()
            );

            System.out.println(
                    "MODE: SINGLE CONTINUOUS AUDIO"
            );

            System.out.println(
                    "========================================"
            );


            // =================================================
            // GENERATE ONE AUDIO FILE
            // =================================================

            Disposable requestDisposable =
                    generateSingleAudioReactive(
                            cleanText
                    )
                            .subscribeOn(
                                    Schedulers.boundedElastic()
                            )
                            .subscribe(

                                    // =========================
                                    // SUCCESS
                                    // =========================

                                    audioUrl -> {

                                        if (
                                                cancelled.get()
                                        ) {

                                            System.out.println(
                                                    "🚫 IGNORING TTS AUDIO - CANCELLED"
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
                                                "✅ GROQ TTS AUDIO READY"
                                        );

                                        System.out.println(
                                                "MODE: SINGLE AUDIO"
                                        );

                                        System.out.println(
                                                "TEXT:"
                                        );

                                        System.out.println(
                                                cleanText
                                        );

                                        System.out.println(
                                                "AUDIO URL:"
                                        );

                                        System.out.println(
                                                audioUrl
                                        );

                                        System.out.println(
                                                "THREAD: "
                                                        + Thread.currentThread()
                                                        .getName()
                                        );

                                        System.out.println(
                                                "========================================"
                                        );


                                        // =========================
                                        // SEND FULL TEXT + ONE AUDIO
                                        // =========================

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
                                                        "❌ TTS CALLBACK ERROR: "
                                                                + callbackError.getMessage()
                                                );
                                            }
                                        }


                                        // =========================
                                        // COMPLETE
                                        // =========================

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
                                                        "❌ TTS COMPLETE CALLBACK ERROR: "
                                                                + callbackError.getMessage()
                                                );
                                            }
                                        }

                                    },


                                    // =========================
                                    // ERROR
                                    // =========================

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
                                                "❌ GROQ TTS ERROR"
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

                                            onError.accept(
                                                    error != null
                                                            ? error
                                                            : new RuntimeException(
                                                            "Unknown TTS error"
                                                    )
                                            );
                                        }
                                    }
                            );


            activeDisposable.set(
                    requestDisposable
            );


            // =================================================
            // RACE CONDITION
            // =================================================

            if (
                    cancelled.get()
            ) {

                requestDisposable.dispose();
            }


        } catch (Exception e) {

            System.err.println(
                    "❌ GROQ TTS INITIALIZATION ERROR: "
                            + e.getMessage()
            );

            if (
                    !cancelled.get() &&
                            onError != null
            ) {

                onError.accept(e);
            }
        }


        return publicDisposable;
    }


    // =========================================================
    // GENERATE SINGLE AUDIO
    // =========================================================

    private String generateSingleAudio(
            String text
    ) {

        try {

            return generateSingleAudioReactive(
                    text
            )
                    .subscribeOn(
                            Schedulers.boundedElastic()
                    )
                    .block();

        } catch (Exception e) {

            System.err.println(
                    "❌ SINGLE GROQ TTS ERROR: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Groq single TTS generation failed",
                    e
            );
        }
    }


    // =========================================================
    // REACTIVE TTS REQUEST
    // =========================================================

    private Mono<String> generateSingleAudioReactive(
            String text
    ) {

        return webClient
                .post()
                .uri(TTS_URL)

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
                                                                        "Groq TTS HTTP "
                                                                                + response.statusCode()
                                                                                + ": "
                                                                                + body
                                                                )
                                                        )
                                        )
                )

                .bodyToMono(
                        byte[].class
                )

                .map(
                        this::saveAudioFile
                );
    }


    // =========================================================
    // SAVE AUDIO
    // =========================================================

    private String saveAudioFile(
            byte[] audioBytes
    ) {

        try {

            if (
                    audioBytes == null ||
                            audioBytes.length == 0
            ) {

                throw new RuntimeException(
                        "Groq returned empty audio"
                );
            }


            Path audioDirectory =
                    Paths.get(uploadPath);

            Files.createDirectories(
                    audioDirectory
            );


            String fileId =
                    UUID.randomUUID()
                            .toString();


            String wavFileName =
                    fileId + ".wav";


            String mp3FileName =
                    fileId + ".mp3";


            Path wavPath =
                    audioDirectory.resolve(
                            wavFileName
                    );


            Path mp3Path =
                    audioDirectory.resolve(
                            mp3FileName
                    );


            // =================================================
            // SAVE WAV
            // =================================================

            Files.write(
                    wavPath,
                    audioBytes
            );


            System.out.println(
                    "💾 GROQ WAV SAVED: "
                            + wavPath.toAbsolutePath()
            );


            // =================================================
            // FFMPEG WAV -> MP3
            // =================================================

            System.out.println(
                    "🎵 STARTING FFMPEG WAV -> MP3"
            );


            ProcessBuilder processBuilder =
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
                    );


            Process process =
                    processBuilder.start();


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
                    "✅ FFMPEG WAV -> MP3 SUCCESS"
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

            } catch (Exception e) {

                System.err.println(
                        "⚠️ WAV DELETE FAILED: "
                                + e.getMessage()
                );
            }


            // =================================================
            // AUDIO URL
            // =================================================

            String audioUrl =
                    "http://10.43.245.75:8080"
                            + "/api/v1.0/audio/"
                            + mp3FileName;


            System.out.println(
                    "🔊 GROQ FULL MP3 AUDIO URL: "
                            + audioUrl
            );


            return audioUrl;


        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to save Groq TTS audio",
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
    // REQUEST DTO
    // =========================================================

    private record GroqTTSRequest(

            String model,

            String input,

            String voice,

            String response_format

    ) {
    }
}