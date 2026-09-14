package com.example.MyFirstApp.controller.ResumeInterviewController;

import com.example.MyFirstApp.DTO.ResumeInterviewDTO.ResumeAnalysisResponse;
import com.example.MyFirstApp.DTO.ResumeInterviewDTO.ResumeInterviewStartRequest;
import com.example.MyFirstApp.DTO.ResumeInterviewDTO.ResumeInterviewStartResponse;
import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewSession;
import com.example.MyFirstApp.service.ResumeInterview.ResumeInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/resume-interview")
@RequiredArgsConstructor
public class ResumeInterviewController {

    private final ResumeInterviewService resumeInterviewService;

    @PostMapping("/analyze")
    public ResumeAnalysisResponse analyzeResume(
            @RequestParam("userId") Long userId,
            @RequestParam("resume") MultipartFile resume
    ) {

        System.out.println("========================================");
        System.out.println("📄 RESUME ANALYZE CONTROLLER");
        System.out.println("USER ID: " + userId);
        System.out.println("FILE NAME: " + resume.getOriginalFilename());
        System.out.println("CONTENT TYPE: " + resume.getContentType());
        System.out.println("SIZE: " + resume.getSize());
        System.out.println("========================================");

        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        if (resume == null || resume.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }

        try {

            return resumeInterviewService.analyzeResume(
                    userId,
                    resume.getBytes(),
                    resume.getOriginalFilename()
            );

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to analyze resume",
                    e
            );
        }
    }

    @PostMapping("/start")
    public ResumeInterviewStartResponse startInterview(
            @RequestParam("userId") Long userId,
            @RequestBody ResumeInterviewStartRequest request
    ) {

        return resumeInterviewService.startInterview(
                userId,
                request
        );
    }

    @GetMapping("/analysis/{analysisId}")
    public ResumeAnalysisResponse getAnalysis(
            @RequestParam("userId") Long userId,
            @PathVariable Long analysisId
    ) {

        return resumeInterviewService.getAnalysis(
                userId,
                analysisId
        );
    }

}