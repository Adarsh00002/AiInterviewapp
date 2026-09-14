package com.example.MyFirstApp.service.ResumeInterview;

import com.example.MyFirstApp.DTO.ResumeInterviewDTO.ResumeAnalysisResponse;
import com.example.MyFirstApp.DTO.ResumeInterviewDTO.ResumeInterviewStartRequest;
import com.example.MyFirstApp.DTO.ResumeInterviewDTO.ResumeInterviewStartResponse;
import com.example.MyFirstApp.DTO.ResumeInterviewDTO.ResumeProjectDTO;

import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewAnalysis;
import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewQuestion;
import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewSession;

import com.example.MyFirstApp.prompt.ResumeInterviewPromt.ResumeAnalysisPrompt;
import com.example.MyFirstApp.prompt.ResumeInterviewPromt.ResumeQuestionPrompt;

import com.example.MyFirstApp.repository.ResumeInterviewRepository.ResumeInterviewAnalysisRepository;
import com.example.MyFirstApp.repository.ResumeInterviewRepository.ResumeInterviewQuestionRepository;
import com.example.MyFirstApp.repository.ResumeInterviewRepository.ResumeInterviewSessionRepository;

import com.example.MyFirstApp.service.GroqService.GroqService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeInterviewService {

    // =========================================================
    // REPOSITORIES
    // =========================================================

    private final ResumeInterviewAnalysisRepository analysisRepository;

    private final ResumeInterviewSessionRepository sessionRepository;

    private final ResumeInterviewQuestionRepository questionRepository;

    // =========================================================
    // AI
    // =========================================================

    private final GroqService groqService;

    // =========================================================
    // JSON
    // =========================================================

    private final ObjectMapper objectMapper =
            new ObjectMapper();


    // =========================================================
    // ANALYZE RESUME
    // =========================================================

    public ResumeAnalysisResponse analyzeResume(
            Long userId,
            byte[] fileBytes,
            String fileName
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "userId cannot be null"
            );
        }

        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException(
                    "Resume file is required"
            );
        }

        if (
                fileName == null ||
                        !fileName.toLowerCase().endsWith(".pdf")
        ) {
            throw new IllegalArgumentException(
                    "Only PDF resumes are supported"
            );
        }

        // =====================================================
        // FILE SIZE
        // =====================================================

        long maxSize =
                5L * 1024L * 1024L;

        if (fileBytes.length > maxSize) {
            throw new IllegalArgumentException(
                    "Resume must be smaller than 5MB"
            );
        }

        // =====================================================
        // EXTRACT PDF TEXT
        // =====================================================

        String resumeText;

        try {

            try (
                    PDDocument document =
                            Loader.loadPDF(fileBytes)
            ) {

                PDFTextStripper stripper =
                        new PDFTextStripper();

                resumeText =
                        stripper
                                .getText(document)
                                .trim();
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to read PDF resume",
                    e
            );
        }

        if (
                resumeText == null ||
                        resumeText.isBlank()
        ) {

            throw new RuntimeException(
                    "Could not extract text from resume"
            );
        }

        // =====================================================
        // AI ANALYSIS
        // =====================================================

        String prompt =
                ResumeAnalysisPrompt.build(
                        resumeText
                );

        String aiResponse =
                groqService.askAI(
                        prompt
                );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "📄 RESUME AI ANALYSIS"
        );

        System.out.println(
                aiResponse
        );

        System.out.println(
                "========================================"
        );

        // =====================================================
        // PARSE JSON
        // =====================================================

        JsonNode root =
                parseJson(
                        aiResponse
                );

        List<String> skills =
                objectMapper.convertValue(
                        root.path("skills"),
                        new TypeReference<List<String>>() {}
                );

        List<ResumeProjectDTO> projects =
                objectMapper.convertValue(
                        root.path("projects"),
                        new TypeReference<List<ResumeProjectDTO>>() {}
                );

        List<String> strengths =
                objectMapper.convertValue(
                        root.path("strengths"),
                        new TypeReference<List<String>>() {}
                );

        List<String> improvements =
                objectMapper.convertValue(
                        root.path("improvements"),
                        new TypeReference<List<String>>() {}
                );

        List<String> interviewFocus =
                objectMapper.convertValue(
                        root.path("interviewFocus"),
                        new TypeReference<List<String>>() {}
                );

        int resumeScore =
                root.path("resumeScore")
                        .asInt(0);

        String overallSummary =
                root.path("overallSummary")
                        .asText("");

        String background =
                root.path("background")
                        .asText("");

        // =====================================================
        // CONVERT TO JSON
        // =====================================================

        String skillsJson;
        String projectsJson;
        String strengthsJson;
        String improvementsJson;
        String interviewFocusJson;

        try {

            skillsJson =
                    objectMapper.writeValueAsString(
                            skills
                    );

            projectsJson =
                    objectMapper.writeValueAsString(
                            projects
                    );

            strengthsJson =
                    objectMapper.writeValueAsString(
                            strengths
                    );

            improvementsJson =
                    objectMapper.writeValueAsString(
                            improvements
                    );

            interviewFocusJson =
                    objectMapper.writeValueAsString(
                            interviewFocus
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to convert resume analysis to JSON",
                    e
            );
        }

        // =====================================================
        // SAVE ANALYSIS
        // =====================================================

        LocalDateTime now =
                LocalDateTime.now();

        ResumeInterviewAnalysis analysis =
                ResumeInterviewAnalysis.builder()

                        .userId(
                                userId
                        )

                        .fileName(
                                fileName
                        )

                        .resumeText(
                                resumeText
                        )

                        .resumeScore(
                                clamp(
                                        resumeScore,
                                        0,
                                        100
                                )
                        )

                        .overallSummary(
                                overallSummary
                        )

                        .background(
                                background
                        )

                        .skillsJson(
                                skillsJson
                        )

                        .projectsJson(
                                projectsJson
                        )

                        .strengthsJson(
                                strengthsJson
                        )

                        .improvementsJson(
                                improvementsJson
                        )

                        .interviewFocusJson(
                                interviewFocusJson
                        )

                        .createdAt(
                                now
                        )

                        .updatedAt(
                                now
                        )

                        .build();

        analysis =
                analysisRepository.save(
                        analysis
                );

        // =====================================================
        // RESPONSE
        // =====================================================

        return ResumeAnalysisResponse
                .builder()

                .analysisId(
                        analysis.getId()
                )

                .fileName(
                        fileName
                )

                .resumeScore(
                        analysis.getResumeScore()
                )

                .overallSummary(
                        overallSummary
                )

                .background(
                        background
                )

                .skills(
                        skills
                )

                .projects(
                        projects
                )

                .strengths(
                        strengths
                )

                .improvements(
                        improvements
                )

                .interviewFocus(
                        interviewFocus
                )

                .build();
    }


    // =========================================================
    // START RESUME INTERVIEW
    // =========================================================

    public ResumeInterviewStartResponse startInterview(
            Long userId,
            ResumeInterviewStartRequest request
    ) {

        if (userId == null) {

            throw new IllegalArgumentException(
                    "userId cannot be null"
            );
        }

        if (
                request == null ||
                        request.getAnalysisId() == null
        ) {

            throw new IllegalArgumentException(
                    "analysisId is required"
            );
        }

        // =====================================================
        // LOAD ANALYSIS
        // =====================================================

        ResumeInterviewAnalysis analysis =
                analysisRepository.findById(
                        request.getAnalysisId()
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Resume analysis not found"
                                )
                );

        // =====================================================
        // OWNERSHIP CHECK
        // =====================================================

        if (
                analysis.getUserId() == null ||
                        !analysis.getUserId().equals(userId)
        ) {

            throw new RuntimeException(
                    "You are not allowed to use this resume analysis"
            );
        }

        // =====================================================
        // SESSION LENGTH
        // =====================================================

        int sessionLength =
                request.getSessionLength() == null ||
                        request.getSessionLength() <= 0
                        ? 10
                        : request.getSessionLength();

        if (
                sessionLength < 1 ||
                        sessionLength > 30
        ) {

            throw new IllegalArgumentException(
                    "Session length must be between 1 and 30 minutes"
            );
        }

        // =====================================================
        // ANALYSIS DATA
        // =====================================================

        String skills =
                analysis.getSkillsJson();

        String projects =
                analysis.getProjectsJson();

        String interviewFocus =
                analysis.getInterviewFocusJson();

        String background =
                analysis.getBackground();

        // =====================================================
        // FIRST QUESTION PROMPT
        // =====================================================

        String firstQuestionPrompt =
                ResumeQuestionPrompt.build(

                        background,

                        skills,

                        projects,

                        interviewFocus,

                        null,

                        null,

                        1
                );

        // =====================================================
        // GENERATE FIRST QUESTION
        // =====================================================

        String firstQuestion =
                groqService.askAI(
                        firstQuestionPrompt
                );

        if (
                firstQuestion == null ||
                        firstQuestion.isBlank()
        ) {

            throw new RuntimeException(
                    "AI returned empty first question"
            );
        }

        firstQuestion =
                firstQuestion.trim();

        // =====================================================
        // CREATE SESSION
        // =====================================================

        ResumeInterviewSession session =
                ResumeInterviewSession.builder()

                        .userId(
                                userId
                        )

                        .analysisId(
                                analysis.getId()
                        )

                        .sessionLength(
                                sessionLength
                        )

                        .currentQuestionNumber(
                                1
                        )

                        .currentQuestion(
                                firstQuestion
                        )

                        .interviewStyle(
                                request.getInterviewStyle()
                        )

                        .completed(
                                false
                        )

                        .startedAt(
                                LocalDateTime.now()
                        )

                        .build();

        session =
                sessionRepository.save(
                        session
                );

        // =====================================================
        // SAVE FIRST QUESTION
        //
        // IMPORTANT:
        // First question bhi question table me save hoga.
        // =====================================================

        ResumeInterviewQuestion firstQuestionEntity =
                ResumeInterviewQuestion.builder()

                        .sessionId(
                                session.getSessionId()
                        )

                        .questionNumber(
                                1
                        )

                        .question(
                                firstQuestion
                        )

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .build();

        questionRepository.save(
                firstQuestionEntity
        );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "✅ RESUME INTERVIEW STARTED"
        );

        System.out.println(
                "SESSION ID: " +
                        session.getSessionId()
        );

        System.out.println(
                "ANALYSIS ID: " +
                        analysis.getId()
        );

        System.out.println(
                "FIRST QUESTION SAVED: " +
                        firstQuestionEntity.getId()
        );

        System.out.println(
                "========================================"
        );

        // =====================================================
        // RESPONSE
        // =====================================================

        return ResumeInterviewStartResponse
                .builder()

                .sessionId(
                        session.getSessionId()
                )

                .analysisId(
                        analysis.getId()
                )

                .currentQuestionNumber(
                        1
                )

                .sessionLength(
                        sessionLength
                )

                .remainingSeconds(
                        sessionLength * 60L
                )

                .firstQuestion(
                        firstQuestion
                )

                .interviewCompleted(
                        false
                )

                .build();
    }


    // =========================================================
    // NEXT QUESTION
    // =========================================================

    public String generateNextQuestion(
            Long sessionId,
            String candidateAnswer
    ) {

        if (
                sessionId == null
        ) {

            throw new IllegalArgumentException(
                    "sessionId cannot be null"
            );
        }

        if (
                candidateAnswer == null ||
                        candidateAnswer.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Candidate answer cannot be empty"
            );
        }

        // =====================================================
        // LOAD SESSION
        // =====================================================

        ResumeInterviewSession session =
                sessionRepository.findById(
                        sessionId
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Resume interview session not found"
                                )
                );

        // =====================================================
        // COMPLETED CHECK
        // =====================================================

        if (
                Boolean.TRUE.equals(
                        session.getCompleted()
                )
        ) {

            throw new RuntimeException(
                    "Interview is already completed"
            );
        }

        // =====================================================
        // TIME CHECK
        // =====================================================

        if (
                isInterviewTimeOver(
                        session
                )
        ) {

            session.setCompleted(
                    true
            );

            session.setCompletedAt(
                    LocalDateTime.now()
            );

            sessionRepository.save(
                    session
            );

            return null;
        }

        // =====================================================
        // LOAD ANALYSIS
        // =====================================================

        ResumeInterviewAnalysis analysis =
                analysisRepository.findById(
                        session.getAnalysisId()
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Resume analysis not found"
                                )
                );

        // =====================================================
        // CURRENT QUESTION NUMBER
        // =====================================================

        int currentQuestionNumber =
                session.getCurrentQuestionNumber() == null
                        ? 1
                        : session.getCurrentQuestionNumber();

        int nextQuestionNumber =
                currentQuestionNumber + 1;

        // =====================================================
        // SAVE ANSWER TO CURRENT QUESTION
        //
        // IMPORTANT:
        // Live service bhi question evaluation karega.
        // Ye method normal REST flow ke liye fallback hai.
        // =====================================================

        questionRepository
                .findFirstBySessionIdAndQuestionNumber(
                        sessionId,
                        currentQuestionNumber
                )
                .ifPresent(
                        question -> {

                            question.setAnswer(
                                    candidateAnswer
                            );

                            question.setAnsweredAt(
                                    LocalDateTime.now()
                            );

                            questionRepository.save(
                                    question
                            );
                        }
                );

        // =====================================================
        // BUILD PROMPT
        // =====================================================

        String prompt =
                ResumeQuestionPrompt.build(

                        analysis.getBackground(),

                        analysis.getSkillsJson(),

                        analysis.getProjectsJson(),

                        analysis.getInterviewFocusJson(),

                        session.getCurrentQuestion(),

                        candidateAnswer,

                        nextQuestionNumber
                );

        // =====================================================
        // GENERATE NEXT QUESTION
        // =====================================================

        String nextQuestion =
                groqService.askAI(
                        prompt
                );

        if (
                nextQuestion == null ||
                        nextQuestion.isBlank()
        ) {

            throw new RuntimeException(
                    "AI returned empty question"
            );
        }

        nextQuestion =
                nextQuestion.trim();

        // =====================================================
        // SAVE NEXT QUESTION
        // =====================================================

        session.setCurrentQuestion(
                nextQuestion
        );

        session.setCurrentQuestionNumber(
                nextQuestionNumber
        );

        sessionRepository.save(
                session
        );

        // =====================================================
        // CREATE NEXT QUESTION RECORD
        // =====================================================

        ResumeInterviewQuestion nextQuestionEntity =
                ResumeInterviewQuestion.builder()

                        .sessionId(
                                sessionId
                        )

                        .questionNumber(
                                nextQuestionNumber
                        )

                        .question(
                                nextQuestion
                        )

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .build();

        questionRepository.save(
                nextQuestionEntity
        );

        return nextQuestion;
    }


    // =========================================================
    // TIME CHECK
    // =========================================================

    private boolean isInterviewTimeOver(
            ResumeInterviewSession session
    ) {

        if (
                session == null
        ) {

            return true;
        }

        if (
                Boolean.TRUE.equals(
                        session.getCompleted()
                )
        ) {

            return true;
        }

        if (
                session.getStartedAt() == null
        ) {

            return false;
        }

        if (
                session.getSessionLength() == null
        ) {

            return false;
        }

        LocalDateTime endTime =
                session
                        .getStartedAt()
                        .plusMinutes(
                                session.getSessionLength()
                        );

        return !LocalDateTime
                .now()
                .isBefore(
                        endTime
                );
    }


    // =========================================================
    // GET ANALYSIS
    // =========================================================

    public ResumeAnalysisResponse getAnalysis(
            Long userId,
            Long analysisId
    ) {

        ResumeInterviewAnalysis analysis =
                analysisRepository.findById(
                        analysisId
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Resume analysis not found"
                                )
                );

        // =====================================================
        // OWNERSHIP CHECK
        // =====================================================

        if (
                analysis.getUserId() == null ||
                        !analysis.getUserId().equals(userId)
        ) {

            throw new RuntimeException(
                    "Unauthorized"
            );
        }

        try {

            return ResumeAnalysisResponse
                    .builder()

                    .analysisId(
                            analysis.getId()
                    )

                    .fileName(
                            analysis.getFileName()
                    )

                    .resumeScore(
                            analysis.getResumeScore()
                    )

                    .overallSummary(
                            analysis.getOverallSummary()
                    )

                    .background(
                            analysis.getBackground()
                    )

                    .skills(
                            objectMapper.readValue(
                                    analysis.getSkillsJson(),
                                    new TypeReference<List<String>>() {}
                            )
                    )

                    .projects(
                            objectMapper.readValue(
                                    analysis.getProjectsJson(),
                                    new TypeReference<List<ResumeProjectDTO>>() {}
                            )
                    )

                    .strengths(
                            objectMapper.readValue(
                                    analysis.getStrengthsJson(),
                                    new TypeReference<List<String>>() {}
                            )
                    )

                    .improvements(
                            objectMapper.readValue(
                                    analysis.getImprovementsJson(),
                                    new TypeReference<List<String>>() {}
                            )
                    )

                    .interviewFocus(
                            objectMapper.readValue(
                                    analysis.getInterviewFocusJson(),
                                    new TypeReference<List<String>>() {}
                            )
                    )

                    .build();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read resume analysis",
                    e
            );
        }
    }


    // =========================================================
    // PARSE AI JSON
    // =========================================================

    private JsonNode parseJson(
            String response
    ) {

        try {

            if (
                    response == null ||
                            response.isBlank()
            ) {

                throw new IllegalArgumentException(
                        "AI response is empty"
                );
            }

            String cleaned =
                    response
                            .replace(
                                    "```json",
                                    ""
                            )
                            .replace(
                                    "```",
                                    ""
                            )
                            .trim();

            return objectMapper.readTree(
                    cleaned
            );

        } catch (Exception e) {

            System.err.println(
                    "❌ INVALID AI JSON:"
            );

            System.err.println(
                    response
            );

            throw new RuntimeException(
                    "AI resume analysis returned invalid JSON",
                    e
            );
        }
    }


    // =========================================================
    // CLAMP
    // =========================================================

    private int clamp(
            int value,
            int min,
            int max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }


    // =========================================================
    // COMPLETE RESUME INTERVIEW
    // =========================================================

    public ResumeInterviewSession completeInterview(
            Long sessionId
    ) {

        if (
                sessionId == null
        ) {

            throw new IllegalArgumentException(
                    "sessionId cannot be null"
            );
        }

        ResumeInterviewSession session =
                sessionRepository.findById(
                        sessionId
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Resume interview session not found"
                                )
                );

        if (
                !Boolean.TRUE.equals(
                        session.getCompleted()
                )
        ) {

            session.setCompleted(
                    true
            );

            session.setCompletedAt(
                    LocalDateTime.now()
            );

            session =
                    sessionRepository.save(
                            session
                    );
        }

        return session;
    }
}