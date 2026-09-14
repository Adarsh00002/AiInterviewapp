package com.example.MyFirstApp.service.ResumeInterview;

import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewAnalysis;
import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewQuestion;
import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewSession;

import com.example.MyFirstApp.prompt.ResumeInterviewPromt.ResumeQuestionPrompt;

import com.example.MyFirstApp.repository.ResumeInterviewRepository.ResumeInterviewAnalysisRepository;
import com.example.MyFirstApp.repository.ResumeInterviewRepository.ResumeInterviewQuestionRepository;
import com.example.MyFirstApp.repository.ResumeInterviewRepository.ResumeInterviewSessionRepository;

import com.example.MyFirstApp.service.ResumeGroqService.ResumeGroqService;
import com.example.MyFirstApp.service.ResumeGroqService.ResumeGroqTTService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ResumeLiveInterviewService {

    // =========================================================
    // REPOSITORIES
    // =========================================================

    private final ResumeInterviewSessionRepository sessionRepository;

    private final ResumeInterviewAnalysisRepository analysisRepository;

    private final ResumeInterviewQuestionRepository questionRepository;

    // =========================================================
    // AI SERVICES
    // =========================================================

    private final ResumeGroqService resumeGroqService;

    private final ResumeGroqTTService resumeGroqTTService;

    // =========================================================
    // JSON
    // =========================================================

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    // =========================================================
    // GENERATION
    // =========================================================

    private final Map<Long, AtomicLong>
            generationMap =
            new ConcurrentHashMap<>();

    // =========================================================
    // ACTIVE PROCESS
    // =========================================================

    private final Map<Long, Disposable>
            activeProcesses =
            new ConcurrentHashMap<>();


    // =========================================================
    // PROCESS ANSWER
    // =========================================================

    public Disposable processAnswer(

            Long sessionId,

            String userAnswer,

            Consumer<String> onAiChunk,

            BiConsumer<String, String> onAudioReady,

            Runnable onComplete,

            Consumer<Throwable> onError

    ) {

        if (sessionId == null) {

            return noOp(
                    onError,
                    new IllegalArgumentException(
                            "sessionId cannot be null"
                    )
            );
        }

        if (
                userAnswer == null
                        || userAnswer.isBlank()
        ) {

            return noOp(
                    onError,
                    new IllegalArgumentException(
                            "User answer cannot be empty"
                    )
            );
        }

        // =====================================================
        // GENERATION
        // =====================================================

        long generation =
                generationMap
                        .computeIfAbsent(
                                sessionId,
                                id ->
                                        new AtomicLong(0)
                        )
                        .incrementAndGet();

        cancelPreviousProcess(
                sessionId
        );

        // =====================================================
        // STATE
        // =====================================================

        AtomicBoolean cancelled =
                new AtomicBoolean(false);

        AtomicReference<Disposable>
                aiStreamReference =
                new AtomicReference<>();

        AtomicReference<Disposable>
                ttsReference =
                new AtomicReference<>();

        AtomicReference<Disposable>
                processReference =
                new AtomicReference<>();

        AtomicReference<StringBuilder>
                aiResponseReference =
                new AtomicReference<>(
                        new StringBuilder()
                );

        // =====================================================
        // PROCESS
        // =====================================================

        Disposable process =
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

                        Disposable ai =
                                aiStreamReference.get();

                        if (ai != null) {
                            ai.dispose();
                        }

                        Disposable tts =
                                ttsReference.get();

                        if (tts != null) {
                            tts.dispose();
                        }

                        Disposable self =
                                processReference.get();

                        if (self != null) {

                            activeProcesses.remove(
                                    sessionId,
                                    self
                            );
                        }
                    }

                    @Override
                    public boolean isDisposed() {

                        return cancelled.get();
                    }
                };

        processReference.set(
                process
        );

        activeProcesses.put(
                sessionId,
                process
        );

        // =====================================================
        // MAIN ASYNC PROCESS
        // =====================================================

        Schedulers
                .boundedElastic()
                .schedule(
                        () -> {

                            try {

                                if (
                                        isCancelled(
                                                sessionId,
                                                generation,
                                                cancelled
                                        )
                                ) {
                                    return;
                                }

                                // =================================================
                                // LOAD SESSION
                                // =================================================

                                ResumeInterviewSession session =
                                        sessionRepository
                                                .findById(
                                                        sessionId
                                                )
                                                .orElseThrow(
                                                        () ->
                                                                new RuntimeException(
                                                                        "Resume interview session not found"
                                                                )
                                                );

                                if (
                                        Boolean.TRUE.equals(
                                                session.getCompleted()
                                        )
                                ) {

                                    throw new RuntimeException(
                                            "Interview already completed"
                                    );
                                }

                                // =================================================
                                // TIME CHECK
                                // =================================================

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

                                    if (
                                            onComplete != null
                                    ) {

                                        onComplete.run();
                                    }

                                    return;
                                }

                                // =================================================
                                // LOAD ANALYSIS
                                // =================================================

                                ResumeInterviewAnalysis analysis =
                                        analysisRepository
                                                .findById(
                                                        session.getAnalysisId()
                                                )
                                                .orElseThrow(
                                                        () ->
                                                                new RuntimeException(
                                                                        "Resume analysis not found"
                                                                )
                                                );

                                // =================================================
                                // CURRENT QUESTION NUMBER
                                // =================================================

                                int questionNumber =
                                        session
                                                .getCurrentQuestionNumber() == null
                                                ? 1
                                                : session
                                                .getCurrentQuestionNumber();

                                // =================================================
                                // SAVE CURRENT QUESTION + ANSWER
                                // =================================================

                                saveCandidateAnswer(

                                        session,

                                        userAnswer
                                );

                                // =================================================
                                // NEXT QUESTION PROMPT
                                // =================================================

                                String livePrompt =
                                        """
                                        You are conducting a realistic resume-based voice interview.

                                        Candidate background:
                                        %s

                                        Candidate skills:
                                        %s

                                        Candidate projects:
                                        %s

                                        Interview focus:
                                        %s

                                        Current question:
                                        %s

                                        Candidate answer:
                                        %s

                                        Continue the interview naturally.

                                        IMPORTANT:
                                        - Briefly react to the candidate answer.
                                        - Then ask exactly ONE next interview question.
                                        - Base the next question on the candidate resume.
                                        - Prefer skills, projects, experience and interview-focus topics.
                                        - Do not invent experience.
                                        - Do not give scoring.
                                        - Do not give a feedback section.
                                        - No markdown.
                                        - No bullets.
                                        - Speak naturally like a human interviewer.
                                        """
                                                .formatted(
                                                        analysis.getBackground(),
                                                        analysis.getSkillsJson(),
                                                        analysis.getProjectsJson(),
                                                        analysis.getInterviewFocusJson(),
                                                        session.getCurrentQuestion(),
                                                        userAnswer
                                                );

                                // =================================================
                                // AI NEXT QUESTION
                                // =================================================

                                Disposable aiStream =
                                        resumeGroqService.streamAI(

                                                livePrompt,

                                                chunk -> {

                                                    if (
                                                            isCancelled(
                                                                    sessionId,
                                                                    generation,
                                                                    cancelled
                                                            )
                                                    ) {
                                                        return;
                                                    }

                                                    synchronized (
                                                            aiResponseReference
                                                    ) {

                                                        aiResponseReference
                                                                .get()
                                                                .append(
                                                                        chunk
                                                                );
                                                    }

                                                    if (
                                                            onAiChunk != null
                                                    ) {

                                                        onAiChunk.accept(
                                                                chunk
                                                        );
                                                    }
                                                },

                                                () -> {

                                                    try {

                                                        if (
                                                                isCancelled(
                                                                        sessionId,
                                                                        generation,
                                                                        cancelled
                                                                )
                                                        ) {
                                                            return;
                                                        }

                                                        // =============================================
                                                        // NEXT QUESTION TEXT
                                                        // =============================================

                                                        String nextQuestion;

                                                        synchronized (
                                                                aiResponseReference
                                                        ) {

                                                            nextQuestion =
                                                                    aiResponseReference
                                                                            .get()
                                                                            .toString()
                                                                            .trim();
                                                        }

                                                        if (
                                                                nextQuestion.isBlank()
                                                        ) {

                                                            throw new RuntimeException(
                                                                    "AI returned empty response"
                                                            );
                                                        }

                                                        // =============================================
                                                        // SAVE NEXT QUESTION
                                                        // =============================================

                                                        ResumeInterviewSession latestSession =
                                                                sessionRepository
                                                                        .findById(
                                                                                sessionId
                                                                        )
                                                                        .orElseThrow(
                                                                                () ->
                                                                                        new RuntimeException(
                                                                                                "Session not found"
                                                                                        )
                                                                        );

                                                        latestSession.setCurrentQuestion(
                                                                nextQuestion
                                                        );

                                                        latestSession.setCurrentQuestionNumber(
                                                                questionNumber + 1
                                                        );

                                                        sessionRepository.save(
                                                                latestSession
                                                        );

                                                        // =============================================
                                                        // TTS
                                                        // =============================================

                                                        if (
                                                                isCancelled(
                                                                        sessionId,
                                                                        generation,
                                                                        cancelled
                                                                )
                                                        ) {
                                                            return;
                                                        }

                                                        Disposable tts =
                                                                resumeGroqTTService
                                                                        .generateSpeechChunks(

                                                                                nextQuestion,

                                                                                (
                                                                                        textChunk,
                                                                                        audioUrl
                                                                                ) -> {

                                                                                    if (
                                                                                            isCancelled(
                                                                                                    sessionId,
                                                                                                    generation,
                                                                                                    cancelled
                                                                                            )
                                                                                    ) {
                                                                                        return;
                                                                                    }

                                                                                    if (
                                                                                            onAudioReady != null
                                                                                    ) {

                                                                                        onAudioReady.accept(
                                                                                                textChunk,
                                                                                                audioUrl
                                                                                        );
                                                                                    }
                                                                                },

                                                                                () -> {

                                                                                    if (
                                                                                            isCancelled(
                                                                                                    sessionId,
                                                                                                    generation,
                                                                                                    cancelled
                                                                                            )
                                                                                    ) {
                                                                                        return;
                                                                                    }

                                                                                    activeProcesses.remove(
                                                                                            sessionId,
                                                                                            process
                                                                                    );

                                                                                    // ==========================================
                                                                                    // EVALUATION
                                                                                    // ==========================================

                                                                                    evaluateSavedQuestion(

                                                                                            sessionId,

                                                                                            questionNumber,

                                                                                            userAnswer,

                                                                                            error -> {

                                                                                                if (
                                                                                                        error != null
                                                                                                                &&
                                                                                                                onError != null
                                                                                                ) {

                                                                                                    onError.accept(
                                                                                                            error
                                                                                                    );

                                                                                                    return;
                                                                                                }

                                                                                                if (
                                                                                                        onComplete != null
                                                                                                ) {

                                                                                                    onComplete.run();
                                                                                                }
                                                                                            }
                                                                                    );
                                                                                },

                                                                                error -> {

                                                                                    if (
                                                                                            isCancelled(
                                                                                                    sessionId,
                                                                                                    generation,
                                                                                                    cancelled
                                                                                            )
                                                                                    ) {
                                                                                        return;
                                                                                    }

                                                                                    activeProcesses.remove(
                                                                                            sessionId,
                                                                                            process
                                                                                    );

                                                                                    if (
                                                                                            onError != null
                                                                                    ) {

                                                                                        onError.accept(
                                                                                                error
                                                                                        );
                                                                                    }
                                                                                }
                                                                        );

                                                        ttsReference.set(
                                                                tts
                                                        );

                                                    } catch (Exception e) {

                                                        activeProcesses.remove(
                                                                sessionId,
                                                                process
                                                        );

                                                        if (
                                                                !cancelled.get()
                                                                        && onError != null
                                                        ) {

                                                            onError.accept(
                                                                    e
                                                            );
                                                        }
                                                    }
                                                },

                                                error -> {

                                                    if (
                                                            isCancelled(
                                                                    sessionId,
                                                                    generation,
                                                                    cancelled
                                                            )
                                                    ) {
                                                        return;
                                                    }

                                                    activeProcesses.remove(
                                                            sessionId,
                                                            process
                                                    );

                                                    if (
                                                            onError != null
                                                    ) {

                                                        onError.accept(
                                                                error
                                                        );
                                                    }
                                                }
                                        );

                                aiStreamReference.set(
                                        aiStream
                                );

                                if (
                                        cancelled.get()
                                ) {

                                    aiStream.dispose();
                                }

                            } catch (Exception e) {

                                activeProcesses.remove(
                                        sessionId,
                                        process
                                );

                                if (
                                        !cancelled.get()
                                                && onError != null
                                ) {

                                    onError.accept(
                                            e
                                    );
                                }
                            }

                        }
                );

        return process;
    }


    // =========================================================
    // SAVE QUESTION + ANSWER
    // =========================================================

    private void saveCandidateAnswer(

            ResumeInterviewSession session,

            String userAnswer

    ) {

        if (
                session == null
                        ||
                        session.getCurrentQuestion() == null
                        ||
                        session.getCurrentQuestion().isBlank()
        ) {

            return;
        }

        int questionNumber =
                session.getCurrentQuestionNumber() == null
                        ? 1
                        : session.getCurrentQuestionNumber();

        ResumeInterviewQuestion question =
                questionRepository
                        .findFirstBySessionIdAndQuestionNumber(
                                session.getSessionId(),
                                questionNumber
                        )
                        .orElse(
                                ResumeInterviewQuestion
                                        .builder()
                                        .sessionId(
                                                session.getSessionId()
                                        )
                                        .questionNumber(
                                                questionNumber
                                        )
                                        .question(
                                                session.getCurrentQuestion()
                                        )
                                        .createdAt(
                                                LocalDateTime.now()
                                        )
                                        .build()
                        );

        question.setAnswer(
                userAnswer
        );

        question.setAnsweredAt(
                LocalDateTime.now()
        );

        questionRepository.save(
                question
        );
    }


    // =========================================================
    // EVALUATE QUESTION
    // =========================================================

    private void evaluateSavedQuestion(

            Long sessionId,

            int questionNumber,

            String userAnswer,

            Consumer<Throwable> callback

    ) {

        try {

            ResumeInterviewSession session =
                    sessionRepository
                            .findById(
                                    sessionId
                            )
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Session not found while evaluating answer"
                                            )
                            );

            ResumeInterviewAnalysis analysis =
                    analysisRepository
                            .findById(
                                    session.getAnalysisId()
                            )
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Resume analysis not found while evaluating answer"
                                            )
                            );

            ResumeInterviewQuestion savedQuestion =
                    questionRepository
                            .findFirstBySessionIdAndQuestionNumber(
                                    sessionId,
                                    questionNumber
                            )
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Saved resume question not found"
                                            )
                            );

            // =================================================
            // EVALUATION PROMPT
            // =================================================

            String evaluationPrompt =
                    """
                    Evaluate the candidate answer for a resume-based interview.

                    Candidate background:
                    %s

                    Candidate skills:
                    %s

                    Candidate projects:
                    %s

                    Interview focus:
                    %s

                    Question:
                    %s

                    Candidate answer:
                    %s

                    Return ONLY valid JSON.

                    JSON format:

                    {
                      "score": 0,
                      "technicalScore": 0,
                      "communicationScore": 0,
                      "problemSolvingScore": 0,
                      "confidenceScore": 0,
                      "feedback": "",
                      "skill": ""
                    }

                    RULES:
                    - Every score must be between 0 and 100.
                    - score must represent the overall quality of this answer.
                    - technicalScore evaluates technical correctness and depth.
                    - communicationScore evaluates clarity and explanation.
                    - problemSolvingScore evaluates reasoning and approach.
                    - confidenceScore evaluates confidence, structure and certainty.
                    - Do not invent experience.
                    - Use null for technicalScore when technical evaluation is not applicable.
                    - feedback must be concise and specific.
                    - Return JSON only.
                    """
                            .formatted(
                                    analysis.getBackground(),
                                    analysis.getSkillsJson(),
                                    analysis.getProjectsJson(),
                                    analysis.getInterviewFocusJson(),
                                    savedQuestion.getQuestion(),
                                    userAnswer
                            );

            StringBuilder evaluationResponse =
                    new StringBuilder();

            resumeGroqService.streamAI(

                    evaluationPrompt,

                    chunk ->
                            evaluationResponse
                                    .append(chunk),

                    () -> {

                        try {

                            String json =
                                    cleanJson(
                                            evaluationResponse
                                                    .toString()
                                    );

                            JsonNode root =
                                    objectMapper.readTree(
                                            json
                                    );

                            // =========================================
                            // PARSE
                            // =========================================

                            Double score =
                                    readDouble(
                                            root,
                                            "score"
                                    );

                            Double technicalScore =
                                    readDouble(
                                            root,
                                            "technicalScore"
                                    );

                            Double communicationScore =
                                    readDouble(
                                            root,
                                            "communicationScore"
                                    );

                            Double problemSolvingScore =
                                    readDouble(
                                            root,
                                            "problemSolvingScore"
                                    );

                            Double confidenceScore =
                                    readDouble(
                                            root,
                                            "confidenceScore"
                                    );

                            String feedback =
                                    root.path(
                                            "feedback"
                                    ).asText(
                                            ""
                                    );

                            String skill =
                                    root.path(
                                            "skill"
                                    ).asText(
                                            ""
                                    );

                            // =========================================
                            // SAVE EVALUATION
                            // =========================================

                            savedQuestion.setScore(
                                    normalize100(
                                            score
                                    )
                            );

                            savedQuestion.setTechnicalScore(
                                    normalizeNullable100(
                                            technicalScore
                                    )
                            );

                            savedQuestion.setCommunicationScore(
                                    normalizeNullable100(
                                            communicationScore
                                    )
                            );

                            savedQuestion.setProblemSolvingScore(
                                    normalizeNullable100(
                                            problemSolvingScore
                                    )
                            );

                            savedQuestion.setConfidenceScore(
                                    normalizeNullable100(
                                            confidenceScore
                                    )
                            );

                            savedQuestion.setFeedback(
                                    feedback
                            );

                            savedQuestion.setSkill(
                                    skill
                            );

                            questionRepository.save(
                                    savedQuestion
                            );

                            if (
                                    callback != null
                            ) {

                                callback.accept(
                                        null
                                );
                            }

                        } catch (Exception e) {

                            if (
                                    callback != null
                            ) {

                                callback.accept(
                                        e
                                );
                            }
                        }
                    },

                    error -> {

                        if (
                                callback != null
                        ) {

                            callback.accept(
                                    error
                            );
                        }
                    }
            );

        } catch (Exception e) {

            if (
                    callback != null
            ) {

                callback.accept(
                        e
                );
            }
        }
    }


    // =========================================================
    // PLAY CURRENT FIRST QUESTION
    // =========================================================

    public Disposable playCurrentQuestion(

            Long sessionId,

            BiConsumer<String, String> onAudioReady,

            Runnable onComplete,

            Consumer<Throwable> onError

    ) {

        ResumeInterviewSession session =
                sessionRepository
                        .findById(
                                sessionId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Resume interview session not found"
                                        )
                        );

        String question =
                session.getCurrentQuestion();

        if (
                question == null
                        || question.isBlank()
        ) {

            return noOp(
                    onError,
                    new RuntimeException(
                            "Current question not found"
                    )
            );
        }

        return resumeGroqTTService.generateSpeechChunks(

                question,

                onAudioReady,

                onComplete,

                onError
        );
    }


    // =========================================================
    // CANCEL CURRENT
    // =========================================================

    public void cancelCurrentProcess(
            Long sessionId
    ) {

        if (
                sessionId == null
        ) {

            return;
        }

        generationMap
                .computeIfAbsent(
                        sessionId,
                        id ->
                                new AtomicLong(0)
                )
                .incrementAndGet();

        Disposable process =
                activeProcesses.remove(
                        sessionId
                );

        if (
                process != null
        ) {

            process.dispose();
        }

        System.out.println(
                "🛑 RESUME LIVE AI CANCELLED: "
                        + sessionId
        );
    }


    // =========================================================
    // CANCEL PREVIOUS
    // =========================================================

    private void cancelPreviousProcess(
            Long sessionId
    ) {

        Disposable process =
                activeProcesses.get(
                        sessionId
                );

        if (
                process != null
        ) {

            process.dispose();
        }
    }


    // =========================================================
    // GENERATION CHECK
    // =========================================================

    private boolean isCancelled(

            Long sessionId,

            long generation,

            AtomicBoolean cancelled

    ) {

        if (
                cancelled.get()
        ) {

            return true;
        }

        AtomicLong current =
                generationMap.get(
                        sessionId
                );

        return current == null
                || current.get() != generation;
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
    // JSON CLEAN
    // =========================================================

    private String cleanJson(
            String response
    ) {

        if (
                response == null
                        ||
                        response.isBlank()
        ) {

            throw new RuntimeException(
                    "AI evaluation returned empty response"
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

        return cleaned;
    }


    // =========================================================
    // READ DOUBLE
    // =========================================================

    private Double readDouble(
            JsonNode root,
            String field
    ) {

        JsonNode node =
                root.path(
                        field
                );

        if (
                node.isMissingNode()
                        ||
                        node.isNull()
        ) {

            return null;
        }

        if (
                !node.isNumber()
        ) {

            return null;
        }

        return node.doubleValue();
    }


    // =========================================================
    // NORMALIZE NULLABLE
    // =========================================================

    private Double normalizeNullable100(
            Double score
    ) {

        if (
                score == null
        ) {

            return null;
        }

        return Math.max(
                0,
                Math.min(
                        100,
                        score
                )
        );
    }


    // =========================================================
    // NORMALIZE 100
    // =========================================================

    private Double normalize100(
            Double score
    ) {

        if (
                score == null
        ) {

            return 0.0;
        }

        if (
                score.isNaN()
                        ||
                        score.isInfinite()
        ) {

            return 0.0;
        }

        return Math.max(
                0,
                Math.min(
                        100,
                        score
                )
        );
    }


    // =========================================================
    // NO OP
    // =========================================================

    private Disposable noOp(

            Consumer<Throwable> onError,

            Throwable error

    ) {

        if (
                onError != null
        ) {

            onError.accept(
                    error
            );
        }

        return new Disposable() {

            @Override
            public void dispose() {
            }

            @Override
            public boolean isDisposed() {
                return true;
            }
        };
    }
}