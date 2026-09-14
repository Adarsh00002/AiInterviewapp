package com.example.MyFirstApp.controller.VoiceInterviewController;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/audio")
public class AudioController {

    @Value("${audio.upload.path}")
    private String uploadPath;

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getAudio(
            @PathVariable String fileName
    ) {

        try {

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "🔊 AUDIO REQUEST RECEIVED"
            );

            System.out.println(
                    "FILE: "
                            + fileName
            );

            System.out.println(
                    "======================================"
            );

            Path audioPath =
                    Paths.get(
                                    uploadPath
                            )
                            .resolve(
                                    fileName
                            )
                            .normalize();

            System.out.println(
                    "📁 AUDIO FILE PATH: "
                            + audioPath.toAbsolutePath()
            );

            // Security: prevent ../ traversal
            Path basePath =
                    Paths.get(
                                    uploadPath
                            )
                            .toAbsolutePath()
                            .normalize();

            if (
                    !audioPath
                            .toAbsolutePath()
                            .normalize()
                            .startsWith(basePath)
            ) {

                System.err.println(
                        "❌ INVALID AUDIO PATH"
                );

                return ResponseEntity
                        .badRequest()
                        .build();
            }

            if (
                    !Files.exists(audioPath)
            ) {

                System.err.println(
                        "❌ AUDIO FILE NOT FOUND"
                );

                System.err.println(
                        "EXPECTED PATH: "
                                + audioPath.toAbsolutePath()
                );

                return ResponseEntity
                        .notFound()
                        .build();
            }

            if (
                    !Files.isRegularFile(audioPath)
            ) {

                System.err.println(
                        "❌ AUDIO PATH IS NOT A FILE"
                );

                return ResponseEntity
                        .notFound()
                        .build();
            }

            Resource resource =
                    new FileSystemResource(
                            audioPath
                    );

            String contentType =
                    Files.probeContentType(
                            audioPath
                    );

            if (
                    contentType == null
            ) {

                if (
                        fileName
                                .toLowerCase()
                                .endsWith(".mp3")
                ) {

                    contentType =
                            "audio/mpeg";

                } else if (
                        fileName
                                .toLowerCase()
                                .endsWith(".wav")
                ) {

                    contentType =
                            "audio/wav";

                } else {

                    contentType =
                            "application/octet-stream";
                }
            }

            System.out.println(
                    "✅ AUDIO FILE FOUND"
            );

            System.out.println(
                    "SIZE: "
                            + Files.size(audioPath)
                            + " bytes"
            );

            System.out.println(
                    "CONTENT TYPE: "
                            + contentType
            );

            System.out.println(
                    "======================================"
            );

            return ResponseEntity
                    .ok()
                    .header(
                            HttpHeaders.ACCEPT_RANGES,
                            "bytes"
                    )
                    .contentType(
                            MediaType.parseMediaType(
                                    contentType
                            )
                    )
                    .contentLength(
                            Files.size(audioPath)
                    )
                    .body(resource);

        } catch (Exception e) {

            System.err.println(
                    "❌ AUDIO SERVING ERROR: "
                            + e.getMessage()
            );

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}