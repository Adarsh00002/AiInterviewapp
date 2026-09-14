package com.example.MyFirstApp.controller.VoiceInterviewController;

import com.example.MyFirstApp.DTO.VoiceInterviewDTO.VoiceInterviewResultResponse;
import com.example.MyFirstApp.DTO.VoiceInterviewDTO.VoiceInterviewStartRequest;
import com.example.MyFirstApp.DTO.VoiceInterviewDTO.VoiceInterviewStartResponse;
import com.example.MyFirstApp.service.Report.VoiceInterviewReportService;
import com.example.MyFirstApp.service.VoiceInterviewService.VoiceInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voice-interview")
@RequiredArgsConstructor
public class VoiceInterviewController {

    private final VoiceInterviewService voiceInterviewService;
    private final VoiceInterviewReportService reportService;

    // =========================================================
    // START
    // =========================================================

    @PostMapping("/start")
    public VoiceInterviewStartResponse startInterview(
            @RequestBody VoiceInterviewStartRequest request
    ) {

        return voiceInterviewService.startInterview(request);
    }

    // =========================================================
    // END INTERVIEW
    // =========================================================

    @PostMapping("/end/{sessionId}")
    public VoiceInterviewResultResponse endInterview(
            @PathVariable Long sessionId
    ) {

        return voiceInterviewService.endInterview(sessionId);
    }

    // =========================================================
    // GET RESULT
    // =========================================================

    @GetMapping("/result/{sessionId}")
    public VoiceInterviewResultResponse getResult(
            @PathVariable Long sessionId
    ) {

        return voiceInterviewService.getResult(sessionId);
    }

    // =========================================================
    // PDF REPORT
    // =========================================================

    @GetMapping("/report/{sessionId}")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable Long sessionId
    ) {

        byte[] pdf =
                reportService.generateReport(sessionId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=voice-interview-report.pdf"
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(pdf);
    }
}