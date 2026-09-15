package com.example.MyFirstApp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // =========================================================
    // IMAGE UPLOAD
    // =========================================================

    public String uploadImage(MultipartFile file) {

        try {

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException(
                        "Image file is empty"
                );
            }

            Map uploadResult =
                    cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.emptyMap()
                    );

            Object secureUrl =
                    uploadResult.get("secure_url");

            if (secureUrl == null) {
                throw new RuntimeException(
                        "Cloudinary did not return image URL"
                );
            }

            return secureUrl.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Image Upload Failed",
                    e
            );
        }
    }


    // =========================================================
    // AUDIO UPLOAD
    // =========================================================

    public String uploadAudio(File audioFile) {

        try {

            if (audioFile == null) {
                throw new IllegalArgumentException(
                        "Audio file cannot be null"
                );
            }

            if (!audioFile.exists()) {
                throw new IllegalArgumentException(
                        "Audio file does not exist: "
                                + audioFile.getAbsolutePath()
                );
            }

            if (!audioFile.isFile()) {
                throw new IllegalArgumentException(
                        "Audio path is not a file: "
                                + audioFile.getAbsolutePath()
                );
            }

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "☁️ CLOUDINARY AUDIO UPLOAD STARTED"
            );

            System.out.println(
                    "FILE: "
                            + audioFile.getAbsolutePath()
            );

            System.out.println(
                    "SIZE: "
                            + audioFile.length()
                            + " bytes"
            );

            System.out.println(
                    "========================================"
            );


            Map uploadResult =
                    cloudinary.uploader().upload(
                            audioFile,
                            ObjectUtils.asMap(
                                    "resource_type",
                                    "video",

                                    "folder",
                                    "ai-interview/conversation/audio"
                            )
                    );


            Object secureUrl =
                    uploadResult.get("secure_url");


            if (secureUrl == null) {

                throw new RuntimeException(
                        "Cloudinary did not return audio secure_url"
                );
            }


            String audioUrl =
                    secureUrl.toString();


            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "✅ CLOUDINARY AUDIO UPLOAD SUCCESS"
            );

            System.out.println(
                    "AUDIO URL: "
                            + audioUrl
            );

            System.out.println(
                    "========================================"
            );


            return audioUrl;

        } catch (Exception e) {

            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "❌ CLOUDINARY AUDIO UPLOAD FAILED"
            );

            System.err.println(
                    "MESSAGE: "
                            + e.getMessage()
            );

            System.err.println(
                    "========================================"
            );

            throw new RuntimeException(
                    "Audio Upload Failed",
                    e
            );
        }
    }
}