
        package com.example.MyFirstApp.service.VoiceInterviewService;

import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewQuestion;
import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewSession;
import com.example.MyFirstApp.repository.voiceInterviewRepository.VoiceInterviewQuestionRepository;
import com.example.MyFirstApp.repository.voiceInterviewRepository.VoiceInterviewSessionRepository;
import com.example.MyFirstApp.service.GroqService.GroqService;
import com.example.MyFirstApp.service.GroqService.GroqTTService;
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
public class LiveInterviewService {

    private final VoiceInterviewSessionRepository sessionRepository;

    private final VoiceInterviewQuestionRepository questionRepository;

    private final GroqService groqService;

    private final GroqTTService groqTTService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================
    // GENERATION
    // =========================================================

    private final Map<Long, AtomicLong> generationMap =
            new ConcurrentHashMap<>();

    // =========================================================
    // ACTIVE PROCESS
    // =========================================================

    private final Map<Long, Disposable> activeProcesses =
            new ConcurrentHashMap<>();

    // =========================================================
    // PROCESS USER ANSWER
    // =========================================================

    public Disposable processAnswer(
            Long sessionId,
            String userAnswer,
            Consumer<String> onChunk,
            BiConsumer<String, String> onAudioReady,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {

        // =====================================================
        // VALIDATION
        // =====================================================

        if (sessionId == null) {

            if (onError != null) {
                onError.accept(
                        new IllegalArgumentException(
                                "sessionId cannot be null"
                        )
                );
            }

            return noOpDisposable();
        }

        if (userAnswer == null || userAnswer.isBlank()) {

            if (onError != null) {
                onError.accept(
                        new IllegalArgumentException(
                                "User answer is empty"
                        )
                );
            }

            return noOpDisposable();
        }

        // =====================================================
        // NEW GENERATION
        // =====================================================

        long generation =
                generationMap
                        .computeIfAbsent(
                                sessionId,
                                id -> new AtomicLong(0)
                        )
                        .incrementAndGet();

        System.out.println(
                "========================================"
        );

        System.out.println(
                "🆕 LIVE INTERVIEW GENERATION: "
                        + generation
        );

        System.out.println(
                "SESSION: "
                        + sessionId
        );

        System.out.println(
                "========================================"
        );

        // =====================================================
        // CANCEL PREVIOUS PROCESS
        // =====================================================

        cancelPreviousProcess(sessionId);

        // =====================================================
        // LOCAL STATE
        // =====================================================

        AtomicBoolean cancelled =
                new AtomicBoolean(false);

        AtomicReference<Disposable> scheduledTask =
                new AtomicReference<>();

        AtomicReference<Disposable> aiStream =
                new AtomicReference<>();

        AtomicReference<Disposable> ttsProcess =
                new AtomicReference<>();

        AtomicReference<Disposable> processReference =
                new AtomicReference<>();

        // =====================================================
        // PROCESS DISPOSABLE
        // =====================================================

        Disposable processDisposable =
                new Disposable() {

                    @Override
                    public void dispose() {

                        if (!cancelled.compareAndSet(false, true)) {
                            return;
                        }

                        System.out.println(
                                "========================================"
                        );

                        System.out.println(
                                "🛑 LIVE AI PROCESS CANCELLED"
                        );

                        System.out.println(
                                "SESSION: "
                                        + sessionId
                        );

                        System.out.println(
                                "GENERATION: "
                                        + generation
                        );

                        System.out.println(
                                "========================================"
                        );

                        Disposable task =
                                scheduledTask.get();

                        if (task != null) {
                            task.dispose();
                        }

                        Disposable stream =
                                aiStream.get();

                        if (stream != null) {
                            stream.dispose();
                        }

                        Disposable tts =
                                ttsProcess.get();

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

        processReference.set(processDisposable);

        activeProcesses.put(
                sessionId,
                processDisposable
        );

        // =====================================================
        // RUN BLOCKING WORK OFF EVENT LOOP
        // =====================================================

        Disposable task =
                Schedulers
                        .boundedElastic()
                        .schedule(() -> {

                            try {

                                // =====================================
                                // CANCEL CHECK
                                // =====================================

                                if (
                                        cancelled.get()
                                                || !isCurrentGeneration(
                                                sessionId,
                                                generation
                                        )
                                ) {
                                    return;
                                }

                                // =====================================
                                // LOAD SESSION
                                // =====================================

                                VoiceInterviewSession session =
                                        sessionRepository
                                                .findById(sessionId)
                                                .orElseThrow(
                                                        () ->
                                                                new RuntimeException(
                                                                        "Session not found: "
                                                                                + sessionId
                                                                )
                                                );

                                // =====================================
                                // COMPLETED CHECK
                                // =====================================

                                if (
                                        Boolean.TRUE.equals(
                                                session.getCompleted()
                                        )
                                ) {

                                    throw new RuntimeException(
                                            "Interview already completed"
                                    );
                                }

                                // =====================================
                                // CURRENT QUESTION
                                // =====================================

                                String currentQuestion =
                                        session.getCurrentQuestion();

                                if (
                                        currentQuestion == null
                                                || currentQuestion.isBlank()
                                ) {

                                    throw new RuntimeException(
                                            "Current question is missing"
                                    );
                                }

                                // =====================================
                                // QUESTION NUMBER
                                // =====================================

                                int questionNumber =
                                        session.getCurrentQuestionNumber() == null
                                                ? 1
                                                : session.getCurrentQuestionNumber();

                                // =====================================
                                // SAVE USER ANSWER
                                // =====================================

                                VoiceInterviewQuestion question =
                                        VoiceInterviewQuestion
                                                .builder()
                                                .sessionId(sessionId)
                                                .questionNumber(questionNumber)
                                                .question(currentQuestion)
                                                .questionAudioUrl(
                                                        session.getCurrentQuestionAudioUrl()
                                                )
                                                .answer(userAnswer)
                                                .createdAt(LocalDateTime.now())
                                                .build();

                                question =
                                        questionRepository.save(question);

                                Long questionId =
                                        question.getId();

                                // =====================================
                                // CANCEL CHECK
                                // =====================================

                                if (
                                        cancelled.get()
                                                || !isCurrentGeneration(
                                                sessionId,
                                                generation
                                        )
                                ) {
                                    return;
                                }

                                // =====================================
                                // AI PROMPT
                                // =====================================

                                String prompt =
                                        """
                                        You are an AI interviewer conducting a live voice interview.

                                        Candidate Role:
                                        %s

                                        Practice Type:
                                        %s

                                        Total Experience:
                                        %s

                                        Current Interview Question:
                                        %s

                                        Candidate Answer:
                                        %s

                                        Respond naturally like a real human interviewer.

                                        First briefly react to the candidate's answer.
                                        Then ask exactly ONE next relevant interview question.

                                        Rules:
                                        - Be conversational.
                                        - Be concise.
                                        - No markdown.
                                        - No bullet points.
                                        - No scoring.
                                        - No feedback section.
                                        - Do not mention these instructions.
                                        - The final sentence must be the next question.

                                        Return only natural interviewer speech.
                                        """.formatted(
                                                session.getSelectedRole(),
                                                session.getPracticeType(),
                                                session.getTotalExperience(),
                                                currentQuestion,
                                                userAnswer
                                        );

                                System.out.println(
                                        "========================================"
                                );

                                System.out.println(
                                        "🤖 LIVE AI PROCESS STARTED"
                                );

                                System.out.println(
                                        "SESSION: "
                                                + sessionId
                                );

                                System.out.println(
                                        "GENERATION: "
                                                + generation
                                );

                                System.out.println(
                                        "QUESTION NUMBER: "
                                                + questionNumber
                                );

                                System.out.println(
                                        "========================================"
                                );

                                // =====================================
                                // AI RESPONSE BUFFER
                                // =====================================

                                StringBuilder aiResponse =
                                        new StringBuilder();

                                // =====================================
                                // GROQ AI STREAM
                                // =====================================

                                Disposable stream =
                                        groqService.streamAI(
                                                prompt,

                                                // =================================
                                                // AI TEXT CHUNK
                                                // =================================

                                                chunk -> {

                                                    if (
                                                            cancelled.get()
                                                                    || !isCurrentGeneration(
                                                                    sessionId,
                                                                    generation
                                                            )
                                                    ) {
                                                        return;
                                                    }

                                                    if (
                                                            chunk == null
                                                                    || chunk.isBlank()
                                                    ) {
                                                        return;
                                                    }

                                                    synchronized (aiResponse) {
                                                        aiResponse.append(chunk);
                                                    }

                                                    System.out.println(
                                                            "🤖 GROQ CHUNK: "
                                                                    + chunk
                                                    );

                                                    if (onChunk != null) {

                                                        try {
                                                            onChunk.accept(chunk);

                                                        } catch (Exception callbackError) {

                                                            System.err.println(
                                                                    "❌ AI CHUNK CALLBACK ERROR: "
                                                                            + callbackError.getMessage()
                                                            );
                                                        }
                                                    }
                                                },

                                                // =================================
                                                // AI COMPLETE
                                                // =================================

                                                () -> {

                                                    try {

                                                        if (
                                                                cancelled.get()
                                                                        || !isCurrentGeneration(
                                                                        sessionId,
                                                                        generation
                                                                )
                                                        ) {
                                                            return;
                                                        }

                                                        String nextQuestion;

                                                        synchronized (aiResponse) {
                                                            nextQuestion =
                                                                    aiResponse
                                                                            .toString()
                                                                            .trim();
                                                        }

                                                        if (nextQuestion.isBlank()) {
                                                            throw new RuntimeException(
                                                                    "AI returned empty response"
                                                            );
                                                        }

                                                        System.out.println(
                                                                "========================================"
                                                        );

                                                        System.out.println(
                                                                "✅ GROQ AI RESPONSE COMPLETED"
                                                        );

                                                        System.out.println(
                                                                "SESSION: "
                                                                        + sessionId
                                                        );

                                                        System.out.println(
                                                                "GENERATION: "
                                                                        + generation
                                                        );

                                                        System.out.println(
                                                                "AI RESPONSE:"
                                                        );

                                                        System.out.println(
                                                                nextQuestion
                                                        );

                                                        System.out.println(
                                                                "========================================"
                                                        );

                                                        // =====================================
                                                        // SAVE NEXT QUESTION
                                                        // =====================================

                                                        session.setCurrentQuestion(
                                                                nextQuestion
                                                        );

                                                        session.setCurrentQuestionNumber(
                                                                questionNumber + 1
                                                        );

                                                        sessionRepository.save(
                                                                session
                                                        );

                                                        System.out.println(
                                                                "✅ NEXT QUESTION SAVED"
                                                        );

                                                        // =====================================
                                                        // START EVALUATION
                                                        // =====================================

                                                        evaluateAnswerAsync(
                                                                sessionId,
                                                                questionId,
                                                                currentQuestion,
                                                                userAnswer
                                                        );

                                                        // =====================================
                                                        // START TTS
                                                        // =====================================

                                                        System.out.println(
                                                                "========================================"
                                                        );

                                                        System.out.println(
                                                                "🔊 STARTING GROQ TTS"
                                                        );

                                                        System.out.println(
                                                                "GENERATION: "
                                                                        + generation
                                                        );

                                                        System.out.println(
                                                                "========================================"
                                                        );

                                                        Disposable tts =
                                                                groqTTService
                                                                        .generateSpeechChunksWithText(
                                                                                nextQuestion,

                                                                                // =============================
                                                                                // TEXT + AUDIO READY
                                                                                // =============================

                                                                                (
                                                                                        textChunk,
                                                                                        audioUrl
                                                                                ) -> {

                                                                                    if (
                                                                                            cancelled.get()
                                                                                                    || !isCurrentGeneration(
                                                                                                    sessionId,
                                                                                                    generation
                                                                                            )
                                                                                    ) {
                                                                                        return;
                                                                                    }

                                                                                    if (
                                                                                            textChunk == null
                                                                                                    || textChunk.isBlank()
                                                                                                    || audioUrl == null
                                                                                                    || audioUrl.isBlank()
                                                                                    ) {
                                                                                        return;
                                                                                    }

                                                                                    System.out.println(
                                                                                            "========================================"
                                                                                    );

                                                                                    System.out.println(
                                                                                            "🔊 TTS CHUNK READY"
                                                                                    );

                                                                                    System.out.println(
                                                                                            "TEXT: "
                                                                                                    + textChunk
                                                                                    );

                                                                                    System.out.println(
                                                                                            "AUDIO: "
                                                                                                    + audioUrl
                                                                                    );

                                                                                    System.out.println(
                                                                                            "========================================"
                                                                                    );

                                                                                    // =====================================================
// SAVE CURRENT QUESTION AUDIO URL
// =====================================================

                                                                                    try {

                                                                                        VoiceInterviewSession latestSession =
                                                                                                sessionRepository
                                                                                                        .findById(sessionId)
                                                                                                        .orElse(null);

                                                                                        if (latestSession != null) {

                                                                                            latestSession.setCurrentQuestionAudioUrl(
                                                                                                    audioUrl
                                                                                            );

                                                                                            sessionRepository.save(
                                                                                                    latestSession
                                                                                            );

                                                                                            System.out.println(
                                                                                                    "✅ CURRENT QUESTION AUDIO URL SAVED"
                                                                                            );

                                                                                            System.out.println(
                                                                                                    "QUESTION NUMBER: "
                                                                                                            + (questionNumber + 1)
                                                                                            );

                                                                                            System.out.println(
                                                                                                    "AUDIO URL: "
                                                                                                            + audioUrl
                                                                                            );
                                                                                        }

                                                                                    } catch (Exception audioSaveError) {

                                                                                        System.err.println(
                                                                                                "⚠️ QUESTION AUDIO URL SAVE ERROR: "
                                                                                                        + audioSaveError.getMessage()
                                                                                        );
                                                                                    }

                                                                                    if (onAudioReady != null) {

                                                                                        try {
                                                                                            onAudioReady.accept(
                                                                                                    textChunk,
                                                                                                    audioUrl
                                                                                            );

                                                                                        } catch (Exception callbackError) {

                                                                                            System.err.println(
                                                                                                    "❌ AUDIO CALLBACK ERROR: "
                                                                                                            + callbackError.getMessage()
                                                                                            );
                                                                                        }
                                                                                    }
                                                                                },

                                                                                // =============================
                                                                                // TTS COMPLETE
                                                                                // =============================

                                                                                () -> {

                                                                                    if (
                                                                                            cancelled.get()
                                                                                                    || !isCurrentGeneration(
                                                                                                    sessionId,
                                                                                                    generation
                                                                                            )
                                                                                    ) {
                                                                                        return;
                                                                                    }

                                                                                    System.out.println(
                                                                                            "✅ TTS COMPLETE FOR QUESTION "
                                                                                                    + (questionNumber + 1)
                                                                                    );

                                                                                    activeProcesses.remove(
                                                                                            sessionId,
                                                                                            processDisposable
                                                                                    );

                                                                                    if (onComplete != null) {

                                                                                        try {
                                                                                            onComplete.run();

                                                                                        } catch (Exception callbackError) {

                                                                                            System.err.println(
                                                                                                    "❌ COMPLETE CALLBACK ERROR: "
                                                                                                            + callbackError.getMessage()
                                                                                            );
                                                                                        }
                                                                                    }
                                                                                },

                                                                                // =============================
                                                                                // TTS ERROR
                                                                                // =============================

                                                                                error -> {

                                                                                    if (
                                                                                            cancelled.get()
                                                                                                    || !isCurrentGeneration(
                                                                                                    sessionId,
                                                                                                    generation
                                                                                            )
                                                                                    ) {
                                                                                        return;
                                                                                    }

                                                                                    System.err.println(
                                                                                            "❌ TTS ERROR: "
                                                                                                    + (
                                                                                                    error == null
                                                                                                            ? "Unknown TTS error"
                                                                                                            : error.getMessage()
                                                                                            )
                                                                                    );

                                                                                    activeProcesses.remove(
                                                                                            sessionId,
                                                                                            processDisposable
                                                                                    );

                                                                                    /*
                                                                                     * TTS failed, but AI text
                                                                                     * already exists.
                                                                                     *
                                                                                     * User should still be
                                                                                     * allowed to continue.
                                                                                     */

                                                                                    if (onComplete != null) {

                                                                                        try {
                                                                                            onComplete.run();

                                                                                        } catch (Exception callbackError) {

                                                                                            System.err.println(
                                                                                                    "❌ COMPLETE CALLBACK ERROR: "
                                                                                                            + callbackError.getMessage()
                                                                                            );
                                                                                        }
                                                                                    }
                                                                                }
                                                                        );

                                                        ttsProcess.set(tts);

                                                        if (
                                                                cancelled.get()
                                                                        || !isCurrentGeneration(
                                                                        sessionId,
                                                                        generation
                                                                )
                                                        ) {
                                                            tts.dispose();
                                                        }

                                                    } catch (Exception e) {

                                                        System.err.println(
                                                                "❌ AI COMPLETE ERROR: "
                                                                        + e.getMessage()
                                                        );

                                                        activeProcesses.remove(
                                                                sessionId,
                                                                processDisposable
                                                        );

                                                        if (
                                                                !cancelled.get()
                                                                        && isCurrentGeneration(
                                                                        sessionId,
                                                                        generation
                                                                )
                                                                        && onError != null
                                                        ) {

                                                            onError.accept(e);
                                                        }
                                                    }
                                                },

                                                // =================================
                                                // AI ERROR
                                                // =================================

                                                error -> {

                                                    if (
                                                            cancelled.get()
                                                                    || !isCurrentGeneration(
                                                                    sessionId,
                                                                    generation
                                                            )
                                                    ) {
                                                        return;
                                                    }

                                                    System.err.println(
                                                            "========================================"
                                                    );

                                                    System.err.println(
                                                            "❌ GROQ LIVE AI ERROR"
                                                    );

                                                    System.err.println(
                                                            error == null
                                                                    ? "Unknown error"
                                                                    : error.getMessage()
                                                    );

                                                    System.err.println(
                                                            "========================================"
                                                    );

                                                    activeProcesses.remove(
                                                            sessionId,
                                                            processDisposable
                                                    );

                                                    if (onError != null) {

                                                        onError.accept(
                                                                error != null
                                                                        ? error
                                                                        : new RuntimeException(
                                                                        "Unknown Groq error"
                                                                )
                                                        );
                                                    }
                                                }
                                        );

                                aiStream.set(stream);

                                // =====================================
                                // RACE CONDITION
                                // =====================================

                                if (
                                        cancelled.get()
                                                || !isCurrentGeneration(
                                                sessionId,
                                                generation
                                        )
                                ) {
                                    stream.dispose();
                                }

                            } catch (Exception e) {

                                if (
                                        cancelled.get()
                                                || !isCurrentGeneration(
                                                sessionId,
                                                generation
                                        )
                                ) {
                                    return;
                                }

                                System.err.println(
                                        "========================================"
                                );

                                System.err.println(
                                        "❌ LIVE PROCESS ERROR"
                                );

                                System.err.println(
                                        e.getMessage()
                                );

                                System.err.println(
                                        "========================================"
                                );

                                activeProcesses.remove(
                                        sessionId,
                                        processDisposable
                                );

                                if (onError != null) {
                                    onError.accept(e);
                                }
                            }
                        });

        scheduledTask.set(task);

        if (cancelled.get()) {
            task.dispose();
        }

        return processDisposable;
    }

    // =========================================================
    // CANCEL CURRENT PROCESS
    // =========================================================

    public void cancelCurrentProcess(Long sessionId) {

        if (sessionId == null) {
            return;
        }

        System.out.println(
                "========================================"
        );

        System.out.println(
                "🛑 CANCELLING CURRENT AI PROCESS"
        );

        System.out.println(
                "SESSION: "
                        + sessionId
        );

        System.out.println(
                "========================================"
        );

        generationMap
                .computeIfAbsent(
                        sessionId,
                        id -> new AtomicLong(0)
                )
                .incrementAndGet();

        Disposable process =
                activeProcesses.remove(sessionId);

        if (process != null) {
            process.dispose();
        }
    }

    // =========================================================
    // CANCEL PREVIOUS PROCESS
    // =========================================================

    private void cancelPreviousProcess(Long sessionId) {

        Disposable previous =
                activeProcesses.get(sessionId);

        if (previous != null) {

            System.out.println(
                    "🛑 PREVIOUS AI PROCESS FOUND"
            );

            previous.dispose();
        }
    }

    // =========================================================
    // GENERATION CHECK
    // =========================================================

    private boolean isCurrentGeneration(
            Long sessionId,
            long generation
    ) {

        AtomicLong current =
                generationMap.get(sessionId);

        if (current == null) {
            return false;
        }

        return current.get() == generation;
    }

    // =========================================================
    // EVALUATION
    // =========================================================

    private void evaluateAnswerAsync(
            Long sessionId,
            Long questionId,
            String question,
            String answer
    ) {

        Schedulers
                .boundedElastic()
                .schedule(() -> {

                    try {

                        System.out.println(
                                "========================================"
                        );

                        System.out.println(
                                "🧠 ANSWER EVALUATION STARTED"
                        );

                        System.out.println(
                                "SESSION: "
                                        + sessionId
                        );

                        System.out.println(
                                "QUESTION ID: "
                                        + questionId
                        );

                        System.out.println(
                                "========================================"
                        );

                        VoiceInterviewSession session =
                                sessionRepository
                                        .findById(sessionId)
                                        .orElse(null);

                        if (session == null) {
                            return;
                        }

                        String evaluationPrompt =
                                """
                                You are an expert technical interviewer evaluating a candidate's answer.

                                Candidate Role:
                                %s

                                Practice Type:
                                %s

                                Total Experience:
                                %s

                                Interview Question:
                                %s

                                Candidate Answer:
                                %s

                                Evaluate independently:

                                1. Communication
                                2. Technical Knowledge
                                3. Problem Solving
                                4. Confidence & Clarity

                                Also provide:
                                - overall score
                                - personalized feedback
                                - primary skill

                                Return ONLY valid JSON.

                                {
                                  "score": 0,
                                  "communicationScore": 0,
                                  "technicalKnowledgeScore": 0,
                                  "problemSolvingScore": 0,
                                  "confidenceClarityScore": 0,
                                  "feedback": "",
                                  "skill": ""
                                }

                                Rules:
                                - Scores must be integers from 0 to 10.
                                - feedback must be specific.
                                - skill must identify the main evaluated skill.
                                - No markdown.
                                - No extra text.
                                """
                                        .formatted(
                                                session.getSelectedRole(),
                                                session.getPracticeType(),
                                                session.getTotalExperience(),
                                                question,
                                                answer
                                        );

                        String json =
                                groqService.askAI(
                                        evaluationPrompt
                                );

                        System.out.println(
                                "📩 EVALUATION RESPONSE:"
                        );

                        System.out.println(json);

                        JsonNode root =
                                objectMapper.readTree(json);

                        VoiceInterviewQuestion savedQuestion =
                                questionRepository
                                        .findById(questionId)
                                        .orElse(null);

                        if (savedQuestion == null) {
                            return;
                        }

                        savedQuestion.setScore(
                                clampScore(
                                        root.path("score").asInt(0)
                                )
                        );

                        savedQuestion.setCommunicationScore(
                                clampScore(
                                        root.path(
                                                "communicationScore"
                                        ).asInt(0)
                                )
                        );

                        savedQuestion.setTechnicalKnowledgeScore(
                                clampScore(
                                        root.path(
                                                "technicalKnowledgeScore"
                                        ).asInt(0)
                                )
                        );

                        savedQuestion.setProblemSolvingScore(
                                clampScore(
                                        root.path(
                                                "problemSolvingScore"
                                        ).asInt(0)
                                )
                        );

                        savedQuestion.setConfidenceClarityScore(
                                clampScore(
                                        root.path(
                                                "confidenceClarityScore"
                                        ).asInt(0)
                                )
                        );

                        savedQuestion.setFeedback(
                                root.path(
                                        "feedback"
                                ).asText("")
                        );

                        savedQuestion.setSkill(
                                root.path("skill")
                                        .asText(
                                                "Technical Knowledge"
                                        )
                        );

                        questionRepository.save(
                                savedQuestion
                        );

                        updateTotalScore(sessionId);

                        System.out.println(
                                "✅ ANSWER EVALUATION SAVED: "
                                        + questionId
                        );

                    } catch (Exception e) {

                        System.err.println(
                                "========================================"
                        );

                        System.err.println(
                                "❌ ANSWER EVALUATION ERROR"
                        );

                        System.err.println(
                                "TYPE: "
                                        + e.getClass().getName()
                        );

                        System.err.println(
                                "MESSAGE: "
                                        + e.getMessage()
                        );

                        System.err.println(
                                "========================================"
                        );

                        e.printStackTrace();
                    }
                });
    }

    // =========================================================
    // TOTAL SCORE
    // =========================================================

    private void updateTotalScore(Long sessionId) {

        VoiceInterviewSession session =
                sessionRepository
                        .findById(sessionId)
                        .orElse(null);

        if (session == null) {
            return;
        }

        int total =
                questionRepository
                        .findBySessionIdOrderByQuestionNumberAsc(
                                sessionId
                        )
                        .stream()
                        .filter(q -> q.getScore() != null)
                        .mapToInt(
                                VoiceInterviewQuestion::getScore
                        )
                        .sum();

        session.setTotalScore(total);

        sessionRepository.save(session);

        System.out.println(
                "✅ TOTAL SCORE UPDATED: "
                        + total
        );
    }

    // =========================================================
    // CLAMP SCORE
    // =========================================================

    private int clampScore(int score) {

        if (score < 0) {
            return 0;
        }

        if (score > 10) {
            return 10;
        }

        return score;
    }

    // =========================================================
    // NO-OP DISPOSABLE
    // =========================================================

    private Disposable noOpDisposable() {

        return new Disposable() {

            private final AtomicBoolean disposed =
                    new AtomicBoolean(true);

            @Override
            public void dispose() {
                disposed.set(true);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
    }
}
