package com.example.MyFirstApp.service.ResumeGroqService;

import com.example.MyFirstApp.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
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

    private final CloudinaryService cloudinaryService;


    // =========================================================
    // GROQ API KEY
    // =========================================================

    @Value("${GROQ_APIKEY}")
    private String apiKey;


    // =========================================================
    // TEMP AUDIO UPLOAD PATH
    // =========================================================

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
    // FULL TEXT
    //      ↓
    // GROQ TTS
    //      ↓
    // WAV
    //      ↓
    // MP3
    //      ↓
    // CLOUDINARY
    //      ↓
    // HTTPS AUDIO URL
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


                        if (current != null) {

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


            if (onError != null) {

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
        // CREATE TEMP DIRECTORY
        // =====================================================

        try {

            Files.createDirectories(
                    Paths.get(
                            uploadPath
                    )
            );

        } catch (Exception e) {

            if (onError != null) {

                onError.accept(
                        e
                );
            }

            return publicDisposable;
        }


        // =====================================================
        // LOG
        // =====================================================

        System.out.println(
                "========================================"
        );

        System.out.println(
                "🔊 RESUME GROQ TTS STARTED"
        );

        System.out.println(
                "MODEL: " + MODEL
        );

        System.out.println(
                "VOICE: " + VOICE
        );

        System.out.println(
                "MODE: ONE COMPLETE AUDIO"
        );

        System.out.println(
                "TEXT LENGTH: " + cleanText.length()
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


                                        if (onError != null) {

                                            onError.accept(
                                                    error
                                            );
                                        }

                                        return;
                                    }


                                    // =================================
                                    // SUCCESS LOG
                                    // =================================

                                    System.out.println(
                                            "========================================"
                                    );

                                    System.out.println(
                                            "✅ RESUME CLOUDINARY AUDIO READY"
                                    );

                                    System.out.println(
                                            "AUDIO URL:"
                                    );

                                    System.out.println(
                                            audioUrl
                                    );

                                    System.out.println(
                                            "TEXT:"
                                    );

                                    System.out.println(
                                            cleanText
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

                                        } catch (Exception callbackError) {

                                            System.err.println(
                                                    "❌ RESUME TTS CALLBACK ERROR: "
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
                // WAV -> MP3 -> CLOUDINARY
                // =================================================

                .map(
                        this::saveAudio
                );
    }


    // =========================================================
    // SAVE WAV -> MP3 -> CLOUDINARY
    // =========================================================

    private String saveAudio(
            byte[] bytes
    ) {

        Path wavPath = null;

        Path mp3Path = null;


        try {

            // =================================================
            // VALIDATE AUDIO
            // =================================================

            if (
                    bytes == null ||
                            bytes.length == 0
            ) {

                throw new RuntimeException(
                        "Empty audio returned from Groq"
                );
            }


            // =================================================
            // TEMP DIRECTORY
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


            wavPath =
                    directory.resolve(
                            "resume_"
                                    + id
                                    + ".wav"
                    );


            mp3Path =
                    directory.resolve(
                            "resume_"
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
                    "💾 RESUME WAV SAVED:"
            );

            System.out.println(
                    wavPath.toAbsolutePath()
            );

            System.out.println(
                    "WAV SIZE: "
                            + bytes.length
                            + " bytes"
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
                        (line = reader.readLine()) != null
                ) {

                    ffmpegOutput
                            .append(line)
                            .append(
                                    System.lineSeparator()
                            );
                }
            }


            // =================================================
            // WAIT FOR FFMPEG
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
                    "✅ RESUME MP3 GENERATED"
            );

            System.out.println(
                    "MP3 SIZE: "
                            + mp3Size
                            + " bytes"
            );


            // =================================================
            // CLOUDINARY UPLOAD
            // =================================================

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "☁️ UPLOADING RESUME AUDIO TO CLOUDINARY"
            );

            System.out.println(
                    "FILE: "
                            + mp3Path.toAbsolutePath()
            );

            System.out.println(
                    "========================================"
            );


            File mp3File =
                    mp3Path.toFile();


            String cloudinaryAudioUrl =
                    cloudinaryService.uploadAudio(
                            mp3File
                    );


            // =================================================
            // VERIFY CLOUDINARY URL
            // =================================================

            if (
                    cloudinaryAudioUrl == null ||
                            cloudinaryAudioUrl.isBlank()
            ) {

                throw new RuntimeException(
                        "Cloudinary returned empty audio URL"
                );
            }


            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "✅ RESUME AUDIO UPLOADED TO CLOUDINARY"
            );

            System.out.println(
                    "CLOUDINARY AUDIO URL:"
            );

            System.out.println(
                    cloudinaryAudioUrl
            );

            System.out.println(
                    "========================================"
            );


            // =================================================
            // DELETE TEMP WAV
            // =================================================

            try {

                Files.deleteIfExists(
                        wavPath
                );

                System.out.println(
                        "🗑️ RESUME TEMP WAV DELETED"
                );

            } catch (Exception e) {

                System.err.println(
                        "⚠️ RESUME WAV DELETE FAILED: "
                                + e.getMessage()
                );
            }


            // =================================================
            // DELETE TEMP MP3
            // =================================================

            try {

                Files.deleteIfExists(
                        mp3Path
                );

                System.out.println(
                        "🗑️ RESUME TEMP MP3 DELETED"
                );

            } catch (Exception e) {

                System.err.println(
                        "⚠️ RESUME MP3 DELETE FAILED: "
                                + e.getMessage()
                );
            }


            // =================================================
            // RETURN CLOUDINARY URL
            // =================================================

            return cloudinaryAudioUrl;


        } catch (Exception e) {

            // =================================================
            // ERROR CLEANUP
            // =================================================

            try {

                if (wavPath != null) {

                    Files.deleteIfExists(
                            wavPath
                    );
                }

            } catch (Exception cleanupError) {

                System.err.println(
                        "⚠️ RESUME WAV CLEANUP FAILED: "
                                + cleanupError.getMessage()
                );
            }


            try {

                if (mp3Path != null) {

                    Files.deleteIfExists(
                            mp3Path
                    );
                }

            } catch (Exception cleanupError) {

                System.err.println(
                        "⚠️ RESUME MP3 CLEANUP FAILED: "
                                + cleanupError.getMessage()
                );
            }


            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "❌ RESUME AUDIO GENERATION FAILED"
            );

            System.err.println(
                    "MESSAGE: "
                            + e.getMessage()
            );

            System.err.println(
                    "========================================"
            );


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