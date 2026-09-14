package com.example.MyFirstApp.controller.VoiceInterviewController;


import com.example.MyFirstApp.service.Report.VoiceInterviewReportService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voiceDownloads")
@RequiredArgsConstructor
public class VoiceInterviewReportController {

    private final VoiceInterviewReportService reportService;

    @GetMapping("/report/{sessionId}")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable Long sessionId
    ) {

        byte[] pdf = reportService.generateReport(sessionId);


        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=voice-interview-report.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}