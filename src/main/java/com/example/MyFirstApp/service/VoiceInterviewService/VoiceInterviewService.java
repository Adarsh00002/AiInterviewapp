
        package com.example.MyFirstApp.service.VoiceInterviewService;

import com.example.MyFirstApp.DTO.VoiceInterviewDTO.QuestionReviewDTO;
import com.example.MyFirstApp.DTO.VoiceInterviewDTO.SkillResultDTO;

import com.example.MyFirstApp.DTO.VoiceInterviewDTO.VoiceInterviewResultResponse;
import com.example.MyFirstApp.DTO.VoiceInterviewDTO.VoiceInterviewStartRequest;
import com.example.MyFirstApp.DTO.VoiceInterviewDTO.VoiceInterviewStartResponse;
import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewQuestion;
import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewSession;
import com.example.MyFirstApp.prompt.VoiceInterviewprompt.VoiceInterviewPromptBuilder;
import com.example.MyFirstApp.repository.voiceInterviewRepository.VoiceInterviewQuestionRepository;
import com.example.MyFirstApp.repository.voiceInterviewRepository.VoiceInterviewSessionRepository;
import com.example.MyFirstApp.service.GroqService.GroqService;
import com.example.MyFirstApp.service.GroqService.GroqTTService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceInterviewService {

    private final VoiceInterviewSessionRepository sessionRepository;

    private final VoiceInterviewQuestionRepository questionRepository;

    private final GroqService groqService;

    private final GroqTTService groqTTService;


    // =========================================================
    // START INTERVIEW
    // =========================================================

    @Transactional
    public VoiceInterviewStartResponse startInterview(
            VoiceInterviewStartRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Interview setup request cannot be null"
            );
        }

        // =====================================================
        // CREATE SESSION
        // =====================================================

        VoiceInterviewSession session =
                VoiceInterviewSession.builder()
                        .userId(request.getUserId())

                        .totalExperience(
                                request.getTotalExperience()
                        )
                        .selectedRole(
                                request.getSelectedRole()
                        )
                        .practiceType(
                                request.getPracticeType()
                        )
                        .sessionLength(
                                request.getSessionLength()
                        )
                        .interviewStyle(
                                request.getInterviewStyle()
                        )

                        .currentQuestionNumber(1)

                        .currentQuestion(null)

                        .totalScore(0)

                        .completed(false)

                        .startedAt(
                                LocalDateTime.now()
                        )

                        .build();

        session =
                sessionRepository.save(
                        session
                );


        // =====================================================
        // GENERATE FIRST QUESTION
        // =====================================================

        String prompt =
                VoiceInterviewPromptBuilder.build(
                        request
                );

        String firstQuestion =
                groqService.askAI(
                        prompt
                );


        if (
                firstQuestion == null
                        || firstQuestion.isBlank()
        ) {

            throw new RuntimeException(
                    "Groq returned empty first interview question"
            );
        }

        firstQuestion =
                firstQuestion.trim();


        // =====================================================
        // SAVE FIRST QUESTION
        // =====================================================

        session.setCurrentQuestion(
                firstQuestion
        );

        session.setCurrentQuestionNumber(
                1
        );

        session =
                sessionRepository.save(
                        session
                );


        // =====================================================
        // GENERATE FIRST QUESTION AUDIO
        //
        // NOTE:
        //
        // This is only for initial REST start.
        //
        // Subsequent live interview turns use:
        //
        // generateSpeechChunks()
        //
        // from LiveInterviewService.
        // =====================================================

        String audioUrl;

        try {

            audioUrl =
                    groqTTService.generateSpeech(
                            firstQuestion
                    );

        } catch (Exception e) {

            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "❌ INITIAL TTS FAILED"
            );

            System.err.println(
                    "SESSION: "
                            + session.getSessionId()
            );

            System.err.println(
                    "ERROR: "
                            + e.getMessage()
            );

            System.err.println(
                    "========================================"
            );

            /*
             * First question text already exists.
             *
             * TTS failure should not destroy the
             * database session.
             *
             * Frontend can still receive the question text.
             */
            audioUrl = null;
        }

        // =====================================================
        // SAVE FIRST QUESTION AUDIO URL
        // =====================================================

        session.setCurrentQuestionAudioUrl(
                audioUrl
        );

        sessionRepository.save(
                session
        );

        System.out.println(
                "✅ FIRST QUESTION AUDIO URL SAVED: "
                        + audioUrl
        );

        // =====================================================
        // LOG
        // =====================================================

        System.out.println(
                "========================================"
        );

        System.out.println(
                "✅ INTERVIEW STARTED"
        );

        System.out.println(
                "SESSION: "
                        + session.getSessionId()
        );

        System.out.println(
                "QUESTION NUMBER: 1"
        );

        System.out.println(
                "QUESTION: "
                        + firstQuestion
        );

        System.out.println(
                "AUDIO: "
                        + audioUrl
        );

        System.out.println(
                "========================================"
        );


        // =====================================================
        // RESPONSE
        // =====================================================

        return VoiceInterviewStartResponse
                .builder()
                .sessionId(
                        session.getSessionId()
                )
                .audioUrl(
                        audioUrl
                )
                .aiSpeech(
                        firstQuestion
                )
                .build();
    }


    // =========================================================
    // END INTERVIEW
    // =========================================================

    @Transactional
    public VoiceInterviewResultResponse endInterview(
            Long sessionId
    ) {

        VoiceInterviewSession session =
                sessionRepository.findById(
                        sessionId
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Interview session not found: "
                                                + sessionId
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

            sessionRepository.save(
                    session
            );
        }


        return buildResult(
                sessionId
        );
    }


    // =========================================================
    // GET RESULT
    // =========================================================

    @Transactional(readOnly = true)
    public VoiceInterviewResultResponse getResult(
            Long sessionId
    ) {

        sessionRepository.findById(
                sessionId
        ).orElseThrow(
                () ->
                        new RuntimeException(
                                "Interview session not found: "
                                        + sessionId
                        )
        );


        return buildResult(
                sessionId
        );
    }


    // =========================================================
    // BUILD FINAL RESULT
    // =========================================================

    @Transactional
    protected VoiceInterviewResultResponse buildResult(
            Long sessionId
    ) {

        VoiceInterviewSession session =
                sessionRepository.findById(
                        sessionId
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Interview session not found: "
                                                + sessionId
                                )
                );


        List<VoiceInterviewQuestion> questions =
                questionRepository
                        .findBySessionIdOrderByQuestionNumberAsc(
                                sessionId
                        );


        // =====================================================
        // TOTAL QUESTIONS
        // =====================================================

        int totalQuestions =
                questions.size();


        // =====================================================
        // ANSWERED QUESTIONS
        // =====================================================

        int answeredQuestions =
                (int)
                        questions.stream()
                                .filter(
                                        q ->
                                                q.getAnswer() != null
                                                        && !q.getAnswer().isBlank()
                                )
                                .count();


        // =====================================================
        // TOTAL SCORE
        // =====================================================

        int totalScore =
                questions.stream()
                        .map(
                                VoiceInterviewQuestion::getScore
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .sum();


        // =====================================================
        // AVERAGE SCORE
        // =====================================================

        double averageScore =
                questions.stream()
                        .map(
                                VoiceInterviewQuestion::getScore
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .average()
                        .orElse(0.0);


        // =====================================================
        // OVERALL SCORE
        // =====================================================

        int overallScore =
                (int)
                        Math.round(
                                averageScore
                        );


        // =====================================================
        // UPDATE SESSION TOTAL
        // =====================================================

        session.setTotalScore(
                totalScore
        );

        sessionRepository.save(
                session
        );


        // =====================================================
        // EVALUATION DIMENSION SCORES
        // =====================================================

        double communicationScore =
                questions.stream()
                        .map(
                                VoiceInterviewQuestion::getCommunicationScore
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .average()
                        .orElse(0.0);


        double technicalKnowledgeScore =
                questions.stream()
                        .map(
                                VoiceInterviewQuestion::getTechnicalKnowledgeScore
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .average()
                        .orElse(0.0);


        double problemSolvingScore =
                questions.stream()
                        .map(
                                VoiceInterviewQuestion::getProblemSolvingScore
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .average()
                        .orElse(0.0);


        double confidenceClarityScore =
                questions.stream()
                        .map(
                                VoiceInterviewQuestion::getConfidenceClarityScore
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .average()
                        .orElse(0.0);


        // =====================================================
        // SKILL RESULTS
        // =====================================================

        List<SkillResultDTO> skills =
                questions.stream()
                        .filter(
                                q ->
                                        q.getSkill() != null
                                                && !q.getSkill().isBlank()
                                                && q.getScore() != null
                        )
                        .collect(
                                Collectors.groupingBy(
                                        VoiceInterviewQuestion::getSkill,
                                        LinkedHashMap::new,
                                        Collectors.averagingInt(
                                                VoiceInterviewQuestion::getScore
                                        )
                                )
                        )
                        .entrySet()
                        .stream()
                        .map(
                                entry ->
                                        SkillResultDTO
                                                .builder()
                                                .name(
                                                        entry.getKey()
                                                )
                                                .skill(
                                                        entry.getKey()
                                                )
                                                .score(
                                                        (int)
                                                                Math.round(
                                                                        entry.getValue()
                                                                )
                                                )
                                                .build()
                        )
                        .toList();


        // =====================================================
        // QUESTION REVIEWS
        // =====================================================

        List<QuestionReviewDTO> reviews =
                questions.stream()
                        .map(
                                q ->
                                        QuestionReviewDTO
                                                .builder()
                                                .questionNumber(
                                                        q.getQuestionNumber()
                                                )
                                                .question(
                                                        q.getQuestion()
                                                )
                                                .score(
                                                        q.getScore()
                                                )
                                                .feedback(
                                                        q.getFeedback()
                                                )
                                                .performance(
                                                        performanceForScore(
                                                                q.getScore()
                                                        )
                                                )
                                                .skill(
                                                        q.getSkill()
                                                )
                                                .questionAudioUrl(
                                                        q.getQuestionAudioUrl()
                                                )
                                                .build()
                        )
                        .toList();


        // =====================================================
        // STRENGTHS / IMPROVEMENTS
        // =====================================================

        List<String> strengths =
                new ArrayList<>();

        List<String> improvements =
                new ArrayList<>();


        addDimensionFeedback(
                questions,
                strengths,
                improvements
        );


        // =====================================================
        // TIME TAKEN
        // =====================================================

        long timeTakenSeconds = 0;


        if (
                session.getStartedAt() != null
        ) {

            LocalDateTime endTime =
                    session.getCompletedAt() != null
                            ? session.getCompletedAt()
                            : LocalDateTime.now();


            timeTakenSeconds =
                    Math.max(
                            0,
                            Duration.between(
                                    session.getStartedAt(),
                                    endTime
                            ).getSeconds()
                    );
        }


        // =====================================================
        // TOTAL SESSION TIME
        // =====================================================

        long totalTimeSeconds =
                session.getSessionLength() == null
                        ? 0
                        : Math.max(
                        0,
                        session.getSessionLength()
                                * 60L
                );


        // =====================================================
        // FINAL RESPONSE
        // =====================================================

        return VoiceInterviewResultResponse
                .builder()

                .sessionId(
                        sessionId
                )

                .selectedRole(
                        session.getSelectedRole()
                )

                .overallScore(
                        overallScore
                )

                .performance(
                        performanceForScore(
                                overallScore
                        )
                )

                .totalQuestions(
                        totalQuestions
                )

                .answeredQuestions(
                        answeredQuestions
                )

                .totalScore(
                        totalScore
                )

                .averageScore(
                        Math.round(
                                averageScore * 100.0
                        ) / 100.0
                )

                // =================================================
                // FOUR EVALUATION SCORES
                // =================================================

                .communicationScore(
                        Math.round(
                                communicationScore * 100.0
                        ) / 100.0
                )

                .technicalKnowledgeScore(
                        Math.round(
                                technicalKnowledgeScore * 100.0
                        ) / 100.0
                )

                .problemSolvingScore(
                        Math.round(
                                problemSolvingScore * 100.0
                        ) / 100.0
                )

                .confidenceClarityScore(
                        Math.round(
                                confidenceClarityScore * 100.0
                        ) / 100.0
                )

                .timeTakenSeconds(
                        timeTakenSeconds
                )

                .totalTimeSeconds(
                        totalTimeSeconds
                )

                .skills(
                        skills
                )

                .strengths(
                        strengths
                )

                .improvements(
                        improvements
                )

                .reviews(
                        reviews
                )

                .build();
    }


    // =========================================================
    // PERFORMANCE
    // =========================================================

    private String performanceForScore(
            Integer score
    ) {

        if (
                score == null
        ) {

            return "Not evaluated";
        }


        if (
                score >= 9
        ) {

            return "Excellent";
        }


        if (
                score >= 8
        ) {

            return "Very Good";
        }


        if (
                score >= 6
        ) {

            return "Good";
        }


        if (
                score >= 4
        ) {

            return "Needs Improvement";
        }


        return "Weak";
    }


    // =========================================================
    // DIMENSION FEEDBACK
    // =========================================================

    private void addDimensionFeedback(
            List<VoiceInterviewQuestion> questions,
            List<String> strengths,
            List<String> improvements
    ) {

        addDimension(
                questions,
                "Communication",
                strengths,
                improvements,
                VoiceInterviewQuestion::getCommunicationScore
        );


        addDimension(
                questions,
                "Technical Knowledge",
                strengths,
                improvements,
                VoiceInterviewQuestion::getTechnicalKnowledgeScore
        );


        addDimension(
                questions,
                "Problem Solving",
                strengths,
                improvements,
                VoiceInterviewQuestion::getProblemSolvingScore
        );


        addDimension(
                questions,
                "Confidence & Clarity",
                strengths,
                improvements,
                VoiceInterviewQuestion::getConfidenceClarityScore
        );
    }


    // =========================================================
    // DIMENSION CALCULATION
    // =========================================================

    private void addDimension(
            List<VoiceInterviewQuestion> questions,
            String name,
            List<String> strengths,
            List<String> improvements,
            Function<
                    VoiceInterviewQuestion,
                    Integer
                    > getter
    ) {

        double average =
                questions.stream()
                        .map(
                                getter
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .average()
                        .orElse(0.0);


        if (
                average >= 8
        ) {

            strengths.add(
                    name
                            + " was one of your stronger areas."
            );

        } else if (
                average > 0
                        && average < 6
        ) {

            improvements.add(
                    name
                            + " needs more improvement and practice."
            );
        }
    }
}

