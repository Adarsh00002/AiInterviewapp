package com.example.MyFirstApp.service.QuickDrill;



import com.example.MyFirstApp.DTO.QuickDrillDTO.*;

import com.example.MyFirstApp.Entity.QuickDrillSession.QuickDrillQuestion;
import com.example.MyFirstApp.Entity.QuickDrillSession.QuickDrillSession;
import com.example.MyFirstApp.prompt.QuickDrill.QuickDrillPrompt;
import com.example.MyFirstApp.repository.QuickDrillSessionRepository.QuickDrillQuestionRepository;
import com.example.MyFirstApp.repository.QuickDrillSessionRepository.QuickDrillSessionRepository;
import com.example.MyFirstApp.service.ResumeGroqService.ResumeGroqService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuickDrillService {

    private final QuickDrillSessionRepository sessionRepository;

    private final QuickDrillQuestionRepository questionRepository;

    private final ResumeGroqService groqService;

    private final ObjectMapper objectMapper =
            new ObjectMapper();


    // =========================================================
    // START DRILL
    // =========================================================

    public QuickDrillStartResponse startDrill(
            Long userId,
            QuickDrillStartRequest request
    ) {

        if (userId == null) {

            throw new IllegalArgumentException(
                    "userId cannot be null"
            );
        }


        if (
                request == null
        ) {

            throw new IllegalArgumentException(
                    "Start drill request cannot be null"
            );
        }


        if (
                request.getTopics() == null ||
                        request.getTopics().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "At least one topic is required"
            );
        }


        // =====================================================
        // CLEAN TOPICS
        // =====================================================

        List<String> topics =
                request.getTopics()
                        .stream()
                        .filter(
                                topic ->
                                        topic != null &&
                                                !topic.isBlank()
                        )
                        .map(
                                String::trim
                        )
                        .distinct()
                        .toList();


        if (
                topics.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "At least one valid topic is required"
            );
        }


        // =====================================================
        // DIFFICULTY
        // =====================================================

        String difficulty =
                request.getDifficulty();

        if (
                difficulty == null ||
                        difficulty.isBlank()
        ) {

            difficulty =
                    "Medium";
        }


        difficulty =
                normalizeDifficulty(
                        difficulty
                );


        // =====================================================
        // QUESTION COUNT
        // =====================================================

        int questionCount =request.getQuestionCount();



        if (
                questionCount < 1 ||
                        questionCount > 30
        ) {

            throw new IllegalArgumentException(
                    "Question count must be between 1 and 30"
            );
        }


        // =====================================================
        // TOPICS JSON
        // =====================================================

        String topicsJson;

        try {

            topicsJson =
                    objectMapper.writeValueAsString(
                            topics
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to serialize topics",
                    e
            );
        }


        // =====================================================
        // CREATE SESSION
        // =====================================================

        QuickDrillSession session =
                QuickDrillSession.builder()

                        .userId(
                                userId
                        )

                        .topicsJson(
                                topicsJson
                        )

                        .difficulty(
                                difficulty
                        )

                        .totalQuestions(
                                questionCount
                        )

                        .currentQuestionNumber(
                                1
                        )

                        .answeredQuestions(
                                0
                        )

                        .totalScore(
                                0.0
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
        // GENERATE FIRST QUESTION
        // =====================================================

        String topicsText =
                String.join(
                        ", ",
                        topics
                );


        String prompt =
                QuickDrillPrompt.buildFirstQuestionPrompt(
                        topicsText,
                        difficulty,
                        1
                );


        String questionText =
                groqService.askAI(
                        prompt
                );


        if (
                questionText == null ||
                        questionText.isBlank()
        ) {

            throw new RuntimeException(
                    "AI returned empty question"
            );
        }


        questionText =
                questionText.trim();


        // =====================================================
        // FIRST TOPIC
        // =====================================================

        String currentTopic =
                topics.get(0);


        // =====================================================
        // SAVE QUESTION
        // =====================================================

        QuickDrillQuestion question =
                QuickDrillQuestion.builder()

                        .drillId(
                                session.getDrillId()
                        )

                        .questionNumber(
                                1
                        )

                        .topic(
                                currentTopic
                        )

                        .difficulty(
                                difficulty
                        )

                        .question(
                                questionText
                        )

                        .evaluated(
                                false
                        )

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .build();


        question =
                questionRepository.save(
                        question
                );


        // =====================================================
        // RESPONSE
        // =====================================================

        return QuickDrillStartResponse
                .builder()

                .drillId(
                        session.getDrillId()
                )
                .questionId(question.getQuestionId())

                .currentQuestionNumber(
                        1
                )

                .totalQuestions(
                        questionCount
                )

                .topic(
                        currentTopic
                )

                .difficulty(
                        difficulty
                )

                .question(
                        question.getQuestion()
                )

                .completed(
                        false
                )

                .build();
    }


    // =========================================================
    // SUBMIT ANSWER
    // =========================================================

    public QuickDrillFeedbackResponse submitAnswer(
            Long userId,
            Long drillId,
            Long questionId,
            String answer
    ) {

        if (
                userId == null
        ) {

            throw new IllegalArgumentException(
                    "userId cannot be null"
            );
        }


        if (
                drillId == null
        ) {

            throw new IllegalArgumentException(
                    "drillId is required"
            );
        }


        if (
                questionId == null
        ) {

            throw new IllegalArgumentException(
                    "questionId is required"
            );
        }


        if (
                answer == null ||
                        answer.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Answer cannot be empty"
            );
        }


        // =====================================================
        // SESSION
        // =====================================================

        QuickDrillSession session =
                sessionRepository
                        .findById(
                                drillId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Quick drill session not found"
                                        )
                        );


        // =====================================================
        // OWNERSHIP
        // =====================================================

        if (
                !session
                        .getUserId()
                        .equals(userId)
        ) {

            throw new RuntimeException(
                    "You are not allowed to access this drill"
            );
        }


        // =====================================================
        // COMPLETED
        // =====================================================

        if (
                Boolean.TRUE.equals(
                        session.getCompleted()
                )
        ) {

            throw new RuntimeException(
                    "Quick drill is already completed"
            );
        }


        // =====================================================
        // QUESTION
        // =====================================================

        QuickDrillQuestion question =
                questionRepository
                        .findById(
                                questionId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Quick drill question not found"
                                        )
                        );


        if (
                !question
                        .getDrillId()
                        .equals(drillId)
        ) {

            throw new RuntimeException(
                    "Question does not belong to this drill"
            );
        }


        // =====================================================
        // DUPLICATE SUBMISSION
        // =====================================================

        if (
                Boolean.TRUE.equals(
                        question.getEvaluated()
                )
        ) {

            return buildFeedbackResponse(
                    session,
                    question
            );
        }


        // =====================================================
        // SAVE ANSWER
        // =====================================================

        question.setUserAnswer(
                answer.trim()
        );


        // =====================================================
        // AI EVALUATION
        // =====================================================

        String prompt =
                QuickDrillPrompt.buildEvaluationPrompt(

                        question.getTopic(),

                        question.getDifficulty(),

                        question.getQuestion(),

                        answer
                );


        String aiResponse =
                groqService.askAI(
                        prompt
                );


        JsonNode root =
                parseJson(
                        aiResponse
                );


        // =====================================================
        // SCORE
        // =====================================================

        double score =
                root.path(
                        "score"
                ).asDouble(
                        0.0
                );


        score =
                Math.max(
                        0.0,
                        Math.min(
                                10.0,
                                score
                        )
                );


        // =====================================================
        // FEEDBACK
        // =====================================================

        List<String> whatYouDidWell =
                objectMapper.convertValue(
                        root.path(
                                "whatYouDidWell"
                        ),
                        new TypeReference<List<String>>() {}
                );


        List<String> whatToImprove =
                objectMapper.convertValue(
                        root.path(
                                "whatToImprove"
                        ),
                        new TypeReference<List<String>>() {}
                );


        String idealAnswer =
                root.path(
                        "idealAnswer"
                ).asText(
                        ""
                );


        // =====================================================
        // SAVE QUESTION
        // =====================================================

        question.setScore(
                score
        );

        question.setWhatYouDidWellJson(
                writeJson(
                        whatYouDidWell
                )
        );

        question.setWhatToImproveJson(
                writeJson(
                        whatToImprove
                )
        );

        question.setIdealAnswer(
                idealAnswer
        );

        question.setEvaluated(
                true
        );

        question.setAnsweredAt(
                LocalDateTime.now()
        );


        questionRepository.save(
                question
        );


        // =====================================================
        // UPDATE SESSION
        // =====================================================

        int answeredCount =
                session.getAnsweredQuestions() == null
                        ? 0
                        : session.getAnsweredQuestions();


        double previousTotal =
                session.getTotalScore() == null
                        ? 0.0
                        : session.getTotalScore();


        session.setAnsweredQuestions(
                answeredCount + 1
        );


        session.setTotalScore(
                previousTotal + score
        );


        boolean lastQuestion =
                question
                        .getQuestionNumber()
                        .equals(
                                session.getTotalQuestions()
                        );


        if (
                lastQuestion
        ) {

            session.setCompleted(
                    true
            );

            session.setCompletedAt(
                    LocalDateTime.now()
            );
        }


        sessionRepository.save(
                session
        );


        return QuickDrillFeedbackResponse
                .builder()

                .drillId(
                        session.getDrillId()
                )

                .questionId(
                        question.getQuestionId()
                )

                .questionNumber(
                        question.getQuestionNumber()
                )

                .totalQuestions(
                        session.getTotalQuestions()
                )

                .topic(
                        question.getTopic()
                )

                .difficulty(
                        question.getDifficulty()
                )

                .question(
                        question.getQuestion()
                )

                .userAnswer(
                        question.getUserAnswer()
                )

                .score(
                        question.getScore()
                )

                .whatYouDidWell(
                        whatYouDidWell
                )

                .whatToImprove(
                        whatToImprove
                )

                .idealAnswer(
                        question.getIdealAnswer()
                )

                .lastQuestion(
                        lastQuestion
                )

                .evaluated(
                        true
                )

                .build();
    }


    // =========================================================
    // NEXT QUESTION
    // =========================================================

    public QuickDrillNextQuestionResponse nextQuestion(
            Long userId,
            Long drillId
    ) {

        QuickDrillSession session =
                sessionRepository
                        .findById(
                                drillId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Quick drill session not found"
                                        )
                        );


        if (
                !session
                        .getUserId()
                        .equals(userId)
        ) {

            throw new RuntimeException(
                    "You are not allowed to access this drill"
            );
        }


        if (
                Boolean.TRUE.equals(
                        session.getCompleted()
                )
        ) {

            throw new RuntimeException(
                    "Quick drill is already completed"
            );
        }


        int currentNumber =
                session
                        .getCurrentQuestionNumber();


        QuickDrillQuestion previousQuestion =
                questionRepository
                        .findByDrillIdAndQuestionNumber(
                                drillId,
                                currentNumber
                        );


        if (
                previousQuestion == null
        ) {

            throw new RuntimeException(
                    "Current question not found"
            );
        }


        if (
                !Boolean.TRUE.equals(
                        previousQuestion.getEvaluated()
                )
        ) {

            throw new RuntimeException(
                    "Submit the current answer before requesting the next question"
            );
        }


        int nextNumber =
                currentNumber + 1;


        if (
                nextNumber >
                        session.getTotalQuestions()
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

            throw new RuntimeException(
                    "No more questions available"
            );
        }


        List<String> topics =
                readTopics(
                        session.getTopicsJson()
                );


        String topicsText =
                String.join(
                        ", ",
                        topics
                );


        String prompt =
                QuickDrillPrompt.buildNextQuestionPrompt(

                        topicsText,

                        session.getDifficulty(),

                        previousQuestion.getQuestion(),

                        previousQuestion.getUserAnswer(),

                        nextNumber
                );


        String nextQuestionText =
                groqService.askAI(
                        prompt
                );


        if (
                nextQuestionText == null ||
                        nextQuestionText.isBlank()
        ) {

            throw new RuntimeException(
                    "AI returned empty next question"
            );
        }


        nextQuestionText =
                nextQuestionText.trim();


        String nextTopic =
                topics.get(
                        (nextNumber - 1)
                                % topics.size()
                );


        QuickDrillQuestion nextQuestion =
                QuickDrillQuestion.builder()

                        .drillId(
                                drillId
                        )

                        .questionNumber(
                                nextNumber
                        )

                        .topic(
                                nextTopic
                        )

                        .difficulty(
                                session.getDifficulty()
                        )

                        .question(
                                nextQuestionText
                        )

                        .evaluated(
                                false
                        )

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .build();


        nextQuestion =
                questionRepository.save(
                        nextQuestion
                );


        session.setCurrentQuestionNumber(
                nextNumber
        );


        sessionRepository.save(
                session
        );


        return QuickDrillNextQuestionResponse
                .builder()

                .drillId(
                        drillId
                )

                .questionId(
                        nextQuestion.getQuestionId()
                )

                .questionNumber(
                        nextNumber
                )

                .totalQuestions(
                        session.getTotalQuestions()
                )

                .topic(
                        nextTopic
                )

                .difficulty(
                        session.getDifficulty()
                )

                .question(
                        nextQuestion.getQuestion()
                )

                .completed(
                        false
                )

                .build();
    }


    // =========================================================
    // GET SESSION
    // =========================================================

    public QuickDrillSession getSession(
            Long userId,
            Long drillId
    ) {

        QuickDrillSession session =
                sessionRepository
                        .findById(
                                drillId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Quick drill session not found"
                                        )
                        );


        if (
                !session
                        .getUserId()
                        .equals(userId)
        ) {

            throw new RuntimeException(
                    "Unauthorized"
            );
        }


        return session;
    }


    // =========================================================
    // GET QUESTIONS
    // =========================================================

    public List<QuickDrillQuestion> getQuestions(
            Long userId,
            Long drillId
    ) {

        QuickDrillSession session =
                getSession(
                        userId,
                        drillId
                );


        return questionRepository
                .findByDrillIdOrderByQuestionNumberAsc(
                        session.getDrillId()
                );
    }


    // =========================================================
    // BUILD EXISTING FEEDBACK
    // =========================================================

    private QuickDrillFeedbackResponse buildFeedbackResponse(
            QuickDrillSession session,
            QuickDrillQuestion question
    ) {

        List<String> whatYouDidWell =
                readStringList(
                        question.getWhatYouDidWellJson()
                );


        List<String> whatToImprove =
                readStringList(
                        question.getWhatToImproveJson()
                );


        boolean lastQuestion =
                question.getQuestionNumber()
                        .equals(
                                session.getTotalQuestions()
                        );


        return QuickDrillFeedbackResponse
                .builder()

                .drillId(
                        session.getDrillId()
                )

                .questionId(
                        question.getQuestionId()
                )

                .questionNumber(
                        question.getQuestionNumber()
                )

                .totalQuestions(
                        session.getTotalQuestions()
                )

                .topic(
                        question.getTopic()
                )

                .difficulty(
                        question.getDifficulty()
                )

                .question(
                        question.getQuestion()
                )

                .userAnswer(
                        question.getUserAnswer()
                )

                .score(
                        question.getScore()
                )

                .whatYouDidWell(
                        whatYouDidWell
                )

                .whatToImprove(
                        whatToImprove
                )

                .idealAnswer(
                        question.getIdealAnswer()
                )

                .lastQuestion(
                        lastQuestion
                )

                .evaluated(
                        question.getEvaluated()
                )

                .build();
    }


    // =========================================================
    // PARSE JSON
    // =========================================================

    private JsonNode parseJson(
            String response
    ) {

        try {

            if (
                    response == null ||
                            response.isBlank()
            ) {

                throw new RuntimeException(
                        "AI returned empty response"
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
                    "❌ QUICK DRILL INVALID AI JSON"
            );

            System.err.println(
                    response
            );

            throw new RuntimeException(
                    "AI returned invalid evaluation JSON",
                    e
            );
        }
    }


    // =========================================================
    // JSON WRITE
    // =========================================================

    private String writeJson(
            Object value
    ) {

        try {

            return objectMapper.writeValueAsString(
                    value
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "JSON conversion failed",
                    e
            );
        }
    }


    // =========================================================
    // JSON READ STRING LIST
    // =========================================================

    private List<String> readStringList(
            String json
    ) {

        if (
                json == null ||
                        json.isBlank()
        ) {

            return List.of();
        }


        try {

            return objectMapper.readValue(
                    json,
                    new TypeReference<List<String>>() {}
            );

        } catch (Exception e) {

            return List.of();
        }
    }


    // =========================================================
    // READ TOPICS
    // =========================================================

    private List<String> readTopics(
            String json
    ) {

        if (
                json == null ||
                        json.isBlank()
        ) {

            return List.of(
                    "General Programming"
            );
        }


        try {

            return objectMapper.readValue(
                    json,
                    new TypeReference<List<String>>() {}
            );

        } catch (Exception e) {

            return List.of(
                    "General Programming"
            );
        }
    }


    // =========================================================
    // NORMALIZE DIFFICULTY
    // =========================================================

    private String normalizeDifficulty(
            String difficulty
    ) {

        String value =
                difficulty
                        .trim()
                        .toLowerCase();


        return switch (value) {

            case "easy" ->
                    "Easy";

            case "hard" ->
                    "Hard";

            case "mixed" ->
                    "Mixed";

            default ->
                    "Medium";
        };
    }

    public QuickDrillResultResponse getResult(
            Long userId,
            Long drillId
    ) {

        QuickDrillSession session =
                sessionRepository
                        .findById(drillId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Quick drill session not found"
                                )
                        );

        if (!session.getUserId().equals(userId)) {

            throw new RuntimeException(
                    "Unauthorized"
            );
        }

        List<QuickDrillQuestion> questions =
                questionRepository
                        .findByDrillIdOrderByQuestionNumberAsc(
                                drillId
                        );

        List<QuickDrillFeedbackResponse> results =
                questions.stream()
                        .filter(
                                q ->
                                        Boolean.TRUE.equals(
                                                q.getEvaluated()
                                        )
                        )
                        .map(
                                q -> QuickDrillFeedbackResponse
                                        .builder()
                                        .drillId(
                                                drillId
                                        )
                                        .questionNumber(
                                                q.getQuestionNumber()
                                        )
                                        .totalQuestions(
                                                session.getTotalQuestions()
                                        )
                                        .topic(
                                                q.getTopic()
                                        )
                                        .difficulty(
                                                q.getDifficulty()
                                        )
                                        .question(
                                                q.getQuestion()
                                        )
                                        .userAnswer(
                                                q.getUserAnswer()
                                        )
                                        .score(
                                                q.getScore()
                                        )
                                        .whatYouDidWell(
                                                readStringList(
                                                        q.getWhatYouDidWellJson()
                                                )
                                        )
                                        .whatToImprove(
                                                readStringList(
                                                        q.getWhatToImproveJson()
                                                )
                                        )
                                        .idealAnswer(
                                                q.getIdealAnswer()
                                        )
                                        .evaluated(
                                                q.getEvaluated()
                                        )
                                        .build()
                        )
                        .toList();

        double totalScore =
                questions.stream()
                        .filter(
                                q ->
                                        q.getScore() != null
                        )
                        .mapToDouble(
                                QuickDrillQuestion::getScore
                        )
                        .sum();

        double averageScore =
                results.isEmpty()
                        ? 0
                        : totalScore / results.size();

        return QuickDrillResultResponse
                .builder()
                .drillId(
                        drillId
                )
                .totalQuestions(
                        session.getTotalQuestions()
                )
                .answeredQuestions(
                        session.getAnsweredQuestions()
                )
                .totalScore(
                        totalScore
                )
                .averageScore(
                        averageScore
                )
                .completed(
                        session.getCompleted()
                )
                .results(
                        results
                )
                .build();
    }
}

