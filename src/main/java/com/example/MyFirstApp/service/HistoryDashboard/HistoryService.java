package com.example.MyFirstApp.service.HistoryDashboard;

import com.example.MyFirstApp.DTO.HistoryDashboard.*;
import com.example.MyFirstApp.DTO.HistoryDashboard.DetailResponse.HistoryDetailResponse;
import com.example.MyFirstApp.DTO.HistoryDashboard.DetailResponse.HistoryQuestionDTO;
import com.example.MyFirstApp.DTO.HistoryDashboard.DetailResponse.HistorySkillDTO;

import com.example.MyFirstApp.Entity.HRandCoummunicationEntity.ConversationMessage;
import com.example.MyFirstApp.Entity.HRandCoummunicationEntity.ConversationSession;

import com.example.MyFirstApp.Entity.QuickDrillSession.QuickDrillQuestion;
import com.example.MyFirstApp.Entity.QuickDrillSession.QuickDrillSession;

import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewAnalysis;
import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewQuestion;
import com.example.MyFirstApp.Entity.ResumeInterviewEntity.ResumeInterviewSession;

import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewQuestion;
import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewSession;

import com.example.MyFirstApp.repository.HRandCoummunicationRespository.ConversationMessageRepositroy;
import com.example.MyFirstApp.repository.HRandCoummunicationRespository.ConversationSessionRepository;

import com.example.MyFirstApp.repository.QuickDrillSessionRepository.QuickDrillQuestionRepository;
import com.example.MyFirstApp.repository.QuickDrillSessionRepository.QuickDrillSessionRepository;

import com.example.MyFirstApp.repository.ResumeInterviewRepository.ResumeInterviewAnalysisRepository;
import com.example.MyFirstApp.repository.ResumeInterviewRepository.ResumeInterviewQuestionRepository;
import com.example.MyFirstApp.repository.ResumeInterviewRepository.ResumeInterviewSessionRepository;

import com.example.MyFirstApp.repository.voiceInterviewRepository.VoiceInterviewQuestionRepository;
import com.example.MyFirstApp.repository.voiceInterviewRepository.VoiceInterviewSessionRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    // =========================================================
    // REPOSITORIES
    // =========================================================

    private final VoiceInterviewSessionRepository voiceSessionRepository;

    private final VoiceInterviewQuestionRepository voiceQuestionRepository;

    private final ResumeInterviewSessionRepository resumeSessionRepository;

    private final ResumeInterviewAnalysisRepository resumeAnalysisRepository;

    private final ResumeInterviewQuestionRepository resumeQuestionRepository;

    private final QuickDrillSessionRepository quickDrillSessionRepository;

    private final QuickDrillQuestionRepository quickDrillQuestionRepository;

    private final ConversationSessionRepository conversationSessionRepository;

    private final ConversationMessageRepositroy conversationMessageRepositroy;


    // =========================================================
    // OBJECT MAPPER
    // =========================================================

    private final ObjectMapper objectMapper =
            new ObjectMapper();


    // =========================================================
    // SUPPORTED TYPES
    // =========================================================

    private List<String> getSupportedTypes() {

        return List.of(
                "VOICE",
                "QUICK_DRILL",
                "COMMUNICATION",
                "HR",
                "RESUME"
        );
    }


    // =========================================================
    // MAIN DASHBOARD
    // =========================================================

    public HistoryDashboardResponse getDashboard(
            Long userId
    ) {

        if (
                userId == null
        ) {

            throw new IllegalArgumentException(
                    "userId is required"
            );
        }


        List<HistoryInterviewDTO> interviews =
                new ArrayList<>();


        // =====================================================
        // VOICE
        // =====================================================

        addVoiceInterviews(
                userId,
                interviews
        );


        // =====================================================
        // RESUME
        // =====================================================

        addResumeInterviews(
                userId,
                interviews
        );


        // =====================================================
        // QUICK DRILL
        // =====================================================

        addQuickDrills(
                userId,
                interviews
        );


        // =====================================================
        // COMMUNICATION / HR
        // =====================================================

        addConversationInterviews(
                userId,
                interviews
        );


        // =====================================================
        // SORT
        // =====================================================

        interviews.sort(
                Comparator.comparing(
                        HistoryInterviewDTO::getCompletedAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );


        // =====================================================
        // COMPLETED ONLY
        // =====================================================

        List<HistoryInterviewDTO> completed =
                interviews.stream()

                        .filter(
                                this::isCompleted
                        )

                        .toList();


        // =====================================================
        // SUMMARY
        // =====================================================

        HistorySummaryDTO summary =
                buildSummary(
                        completed
                );


        // =====================================================
        // PERFORMANCE
        // =====================================================

        List<PerformancePointDTO> performance =
                buildPerformance(
                        completed
                );


        // =====================================================
        // TYPE-WISE SKILLS
        // =====================================================

        Map<String, List<SkillPerformanceDTO>> typeWiseSkills =
                buildTypeWiseSkills(
                        userId
                );


        // =====================================================
        // STRENGTHS
        // =====================================================

        Map<String, List<SkillPerformanceDTO>> strengths =
                buildTypeWiseStrengths(
                        typeWiseSkills
                );


        // =====================================================
        // IMPROVEMENTS
        // =====================================================

        Map<String, List<SkillPerformanceDTO>> improvements =
                buildTypeWiseImprovements(
                        typeWiseSkills
                );


        // =====================================================
        // STREAK
        // =====================================================

        StreakDTO streak =
                buildStreak(
                        completed
                );


        // =====================================================
        // AI INSIGHT
        // =====================================================

        AIInsightDTO insight =
                buildInsight(
                        completed
                );


        // =====================================================
        // RECENT
        // =====================================================

        List<HistoryInterviewDTO> recent =
                completed.stream()
                        .limit(20)
                        .toList();


        // =====================================================
        // RESPONSE
        // =====================================================

        return HistoryDashboardResponse.builder()

                .summary(
                        summary
                )

                .performance(
                        performance
                )

                .recentInterviews(
                        recent
                )

                .strengths(
                        strengths
                )

                .improvements(
                        improvements
                )

                .streak(
                        streak
                )

                .insight(
                        insight
                )

                .build();
    }


    // =========================================================
    // VOICE INTERVIEWS
    // =========================================================

    private void addVoiceInterviews(
            Long userId,
            List<HistoryInterviewDTO> output
    ) {

        List<VoiceInterviewSession> sessions =
                voiceSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                VoiceInterviewSession session :
                sessions
        ) {

            if (
                    session == null
                            ||
                            !isVoiceCompleted(session)
            ) {

                continue;
            }


            List<VoiceInterviewQuestion> questions =
                    voiceQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            Double rawScore =
                    questions.stream()

                            .filter(
                                    q ->
                                            q != null
                                                    &&
                                                    q.getScore() != null
                            )

                            .mapToDouble(
                                    q ->
                                            q.getScore()
                            )

                            .average()

                            .orElseGet(
                                    () ->
                                            safeVoiceScore(
                                                    session
                                            )
                            );


            double score =
                    normalizeScore(
                            rawScore
                    );


            String title =
                    safeString(
                            session.getSelectedRole(),
                            "Voice Interview"
                    );


            LocalDateTime completedAt =
                    session.getCompletedAt();


            output.add(

                    HistoryInterviewDTO.builder()

                            .id(
                                    session.getSessionId()
                            )

                            .title(
                                    title
                            )

                            .type(
                                    "VOICE"
                            )

                            .score(
                                    round(score)
                            )

                            .completedAt(
                                    completedAt
                            )

                            .durationSeconds(
                                    durationSeconds(
                                            session.getStartedAt(),
                                            completedAt
                                    )
                            )

                            .tag(
                                    scoreTag(
                                            score
                                    )
                            )

                            .build()
            );
        }
    }


    // =========================================================
    // RESUME INTERVIEWS
    // =========================================================

    private void addResumeInterviews(
            Long userId,
            List<HistoryInterviewDTO> output
    ) {

        List<ResumeInterviewSession> sessions =
                resumeSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                ResumeInterviewSession session :
                sessions
        ) {

            if (
                    session == null
                            ||
                            !isResumeCompleted(session)
            ) {

                continue;
            }


            double score =
                    0.0;


            // =================================================
            // FIRST:
            // ACTUAL INTERVIEW QUESTION SCORES
            // =================================================

            List<ResumeInterviewQuestion> questions =
                    resumeQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            double questionAverage =
                    questions.stream()

                            .filter(
                                    question ->
                                            question != null
                                                    &&
                                                    question.getScore() != null
                            )

                            .mapToDouble(
                                    question ->
                                            normalizeScore(
                                                    question.getScore()
                                            )
                            )

                            .average()

                            .orElse(-1.0);


            if (
                    questionAverage >= 0
            ) {

                score =
                        questionAverage;

            } else if (
                    session.getAnalysisId() != null
            ) {

                // =================================================
                // FALLBACK:
                // RESUME ANALYSIS SCORE
                // =================================================

                Optional<ResumeInterviewAnalysis> analysis =
                        resumeAnalysisRepository.findById(
                                session.getAnalysisId()
                        );


                if (
                        analysis.isPresent()
                                &&
                                analysis.get().getResumeScore() != null
                ) {

                    score =
                            normalizeScore(
                                    analysis
                                            .get()
                                            .getResumeScore()
                                            .doubleValue()
                            );
                }
            }


            LocalDateTime completedAt =
                    session.getCompletedAt();


            output.add(

                    HistoryInterviewDTO.builder()

                            .id(
                                    session.getSessionId()
                            )

                            .title(
                                    "Resume Interview"
                            )

                            .type(
                                    "RESUME"
                            )

                            .score(
                                    round(score)
                            )

                            .completedAt(
                                    completedAt
                            )

                            .durationSeconds(
                                    durationSeconds(
                                            session.getStartedAt(),
                                            completedAt
                                    )
                            )

                            .tag(
                                    scoreTag(
                                            score
                                    )
                            )

                            .build()
            );
        }
    }


    // =========================================================
    // QUICK DRILL
    // =========================================================

    private void addQuickDrills(
            Long userId,
            List<HistoryInterviewDTO> output
    ) {

        List<QuickDrillSession> sessions =
                quickDrillSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                QuickDrillSession session :
                sessions
        ) {

            if (
                    session == null
                            ||
                            !isQuickDrillCompleted(session)
            ) {

                continue;
            }


            List<QuickDrillQuestion> questions =
                    quickDrillQuestionRepository
                            .findByDrillIdOrderByQuestionNumberAsc(
                                    session.getDrillId()
                            );


            Double rawScore =
                    questions.stream()

                            .filter(
                                    q ->
                                            q != null
                                                    &&
                                                    q.getScore() != null
                            )

                            .mapToDouble(
                                    q ->
                                            q.getScore()
                            )

                            .average()

                            .orElseGet(
                                    () ->
                                            safeQuickScore(
                                                    session
                                            )
                            );


            double score =
                    normalizeScore(
                            rawScore
                    );


            LocalDateTime completedAt =
                    session.getCompletedAt();


            output.add(

                    HistoryInterviewDTO.builder()

                            .id(
                                    session.getDrillId()
                            )

                            .title(
                                    "Quick Drill"
                            )

                            .type(
                                    "QUICK_DRILL"
                            )

                            .score(
                                    round(score)
                            )

                            .completedAt(
                                    completedAt
                            )

                            .durationSeconds(
                                    durationSeconds(
                                            session.getStartedAt(),
                                            completedAt
                                    )
                            )

                            .tag(
                                    scoreTag(
                                            score
                                    )
                            )

                            .build()
            );
        }
    }


    // =========================================================
    // COMMUNICATION / HR
    // =========================================================

    // =========================================================
// COMMUNICATION / HR
// =========================================================

    private void addConversationInterviews(
            Long userId,
            List<HistoryInterviewDTO> output
    ) {

        List<ConversationSession> sessions =
                conversationSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );

        for (
                ConversationSession session :
                sessions
        ) {

            if (
                    session == null
                            ||
                            session.getEndedAt() == null
            ) {

                continue;
            }

            String type =
                    getConversationType(
                            session
                    );

            double score =
                    getConversationScore(
                            session,
                            conversationMessageRepositroy
                                    .findBySessionIdOrderByCreatedAtAsc(
                                            session.getId()
                                    )
                    );

            String title =
                    "HR".equalsIgnoreCase(type)
                            ? "HR Interview"
                            : "Communication Interview";

            output.add(

                    HistoryInterviewDTO.builder()

                            .id(
                                    session.getId()
                            )

                            .title(
                                    title
                            )

                            .type(
                                    type
                            )

                            .score(
                                    round(score)
                            )

                            .completedAt(
                                    session.getEndedAt()
                            )

                            .durationSeconds(
                                    durationSeconds(
                                            session.getStartedAt(),
                                            session.getEndedAt()
                                    )
                            )

                            .tag(
                                    scoreTag(
                                            score
                                    )
                            )

                            .build()
            );
        }
    }

    // =========================================================
    // CONVERSATION TYPE
    // =========================================================

    private String getConversationType(
            ConversationSession session
    ) {

        if (
                session == null
                        ||
                        session.getMode() == null
        ) {

            return "COMMUNICATION";
        }


        if (
                "HR".equalsIgnoreCase(
                        session.getMode().name()
                )
        ) {

            return "HR";
        }


        return "COMMUNICATION";
    }


    // =========================================================
    // CONVERSATION SCORE
    // =========================================================

    // =========================================================
// CONVERSATION SCORE
// =========================================================

    private double getConversationScore(

            ConversationSession session,

            List<ConversationMessage> messages

    ) {

        // =====================================================
        // FIRST PRIORITY:
        // ACTUAL FINAL SESSION SCORE
        // =====================================================

        if (
                session != null
                        &&
                        session.getOverallScore() != null
        ) {

            return normalizeScore(
                    session.getOverallScore()
            );
        }

        // =====================================================
        // FALLBACK:
        // USER MESSAGE SCORES
        // =====================================================

        if (
                messages != null
                        &&
                        !messages.isEmpty()
        ) {

            double average =
                    messages.stream()

                            .filter(
                                    Objects::nonNull
                            )

                            .filter(
                                    message ->
                                            message.getSender() != null
                                                    &&
                                                    "USER".equalsIgnoreCase(
                                                            message.getSender().name()
                                                    )
                            )

                            .map(
                                    ConversationMessage::getScore
                            )

                            .filter(
                                    Objects::nonNull
                            )

                            .mapToDouble(
                                    Double::doubleValue
                            )

                            .average()

                            .orElse(0.0);

            return normalizeScore(
                    average
            );
        }

        return 0.0;
    }


    // =========================================================
    // SUMMARY
    // =========================================================

    private HistorySummaryDTO buildSummary(
            List<HistoryInterviewDTO> interviews
    ) {

        if (
                interviews == null
                        ||
                        interviews.isEmpty()
        ) {

            return HistorySummaryDTO.builder()

                    .score(0.0)

                    .change(0.0)

                    .interviews(0L)

                    .streak(0)

                    .practiceTime("0m")

                    .build();
        }


        double average =
                interviews.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .map(
                                HistoryInterviewDTO::getScore
                        )

                        .filter(
                                Objects::nonNull
                        )

                        .mapToDouble(
                                Double::doubleValue
                        )

                        .average()

                        .orElse(0.0);


        double change =
                calculateMonthlyChange(
                        interviews
                );


        long totalSeconds =
                interviews.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .mapToLong(
                                item ->
                                        item.getDurationSeconds() == null
                                                ? 0L
                                                : item.getDurationSeconds()
                        )

                        .sum();


        int streak =
                calculateStreakDays(
                        interviews
                );


        return HistorySummaryDTO.builder()

                .score(
                        round(
                                average
                        )
                )

                .change(
                        round(
                                change
                        )
                )

                .interviews(
                        (long) interviews.size()
                )

                .streak(
                        streak
                )

                .practiceTime(
                        formatDuration(
                                totalSeconds
                        )
                )

                .build();
    }


    // =========================================================
    // PERFORMANCE
    // =========================================================

    private List<PerformancePointDTO> buildPerformance(
            List<HistoryInterviewDTO> interviews
    ) {

        if (
                interviews == null
                        ||
                        interviews.isEmpty()
        ) {

            return List.of();
        }


        return interviews.stream()

                .filter(
                        Objects::nonNull
                )

                .filter(
                        item ->
                                item.getCompletedAt() != null
                )

                .sorted(
                        Comparator.comparing(
                                HistoryInterviewDTO::getCompletedAt,
                                Comparator.reverseOrder()
                        )
                )

                .limit(30)

                .sorted(
                        Comparator.comparing(
                                HistoryInterviewDTO::getCompletedAt
                        )
                )

                .map(
                        item ->
                                PerformancePointDTO.builder()

                                        .date(
                                                item.getCompletedAt()
                                                        .toLocalDate()
                                        )

                                        .score(
                                                round(
                                                        normalizeScore(
                                                                item.getScore()
                                                        )
                                                )
                                        )

                                        .build()
                )

                .toList();
    }


    // =========================================================
    // TYPE-WISE SKILLS
    // =========================================================

    private Map<String, List<SkillPerformanceDTO>> buildTypeWiseSkills(
            Long userId
    ) {

        Map<String, Map<String, List<Double>>> typeSkillScores =
                new LinkedHashMap<>();


        for (
                String type :
                getSupportedTypes()
        ) {

            typeSkillScores.put(
                    type,
                    new LinkedHashMap<>()
            );
        }


        // =====================================================
        // VOICE
        // =====================================================

        List<VoiceInterviewSession> voiceSessions =
                voiceSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                VoiceInterviewSession session :
                voiceSessions
        ) {

            if (
                    session == null
                            ||
                            !isVoiceCompleted(session)
            ) {

                continue;
            }


            List<VoiceInterviewQuestion> questions =
                    voiceQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            for (
                    VoiceInterviewQuestion question :
                    questions
            ) {

                if (
                        question == null
                                ||
                                question.getSkill() == null
                                ||
                                question.getSkill().isBlank()
                                ||
                                question.getScore() == null
                ) {

                    continue;
                }


                addSkillScore(

                        typeSkillScores.get("VOICE"),

                        question.getSkill().trim(),

                        normalizeScore(
                                question.getScore().doubleValue()
                        )
                );
            }
        }


        // =====================================================
        // QUICK DRILL
        // =====================================================

        List<QuickDrillSession> drills =
                quickDrillSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                QuickDrillSession drill :
                drills
        ) {

            if (
                    drill == null
                            ||
                            !isQuickDrillCompleted(drill)
            ) {

                continue;
            }


            List<QuickDrillQuestion> questions =
                    quickDrillQuestionRepository
                            .findByDrillIdOrderByQuestionNumberAsc(
                                    drill.getDrillId()
                            );


            for (
                    QuickDrillQuestion question :
                    questions
            ) {

                if (
                        question == null
                                ||
                                question.getTopic() == null
                                ||
                                question.getTopic().isBlank()
                                ||
                                question.getScore() == null
                ) {

                    continue;
                }


                addSkillScore(

                        typeSkillScores.get("QUICK_DRILL"),

                        question.getTopic().trim(),

                        normalizeScore(
                                question.getScore()
                        )
                );
            }
        }


        // =====================================================
        // COMMUNICATION / HR
        // =====================================================

        List<ConversationSession> conversations =
                conversationSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                ConversationSession session :
                conversations
        ) {

            if (
                    session == null
                            ||
                            session.getEndedAt() == null
            ) {

                continue;
            }


            String bucket =
                    getConversationType(
                            session
                    );


            /*
             * Use session-level actual scores.
             */

            if (
                    session.getOverallScore() != null
            ) {

                addSkillScore(
                        typeSkillScores.get(bucket),
                        "Overall Performance",
                        normalizeScore(
                                session.getOverallScore()
                        )
                );
            }


            if (
                    session.getCommunicationScore() != null
            ) {

                addSkillScore(
                        typeSkillScores.get(bucket),
                        "Communication",
                        normalizeScore(
                                session.getCommunicationScore()
                        )
                );
            }


            if (
                    session.getTechnicalScore() != null
            ) {

                addSkillScore(
                        typeSkillScores.get(bucket),
                        "Technical",
                        normalizeScore(
                                session.getTechnicalScore()
                        )
                );
            }


            if (
                    session.getProblemSolvingScore() != null
            ) {

                addSkillScore(
                        typeSkillScores.get(bucket),
                        "Problem Solving",
                        normalizeScore(
                                session.getProblemSolvingScore()
                        )
                );
            }


            if (
                    session.getConfidenceScore() != null
            ) {

                addSkillScore(
                        typeSkillScores.get(bucket),
                        "Confidence",
                        normalizeScore(
                                session.getConfidenceScore()
                        )
                );
            }
        }


        // =====================================================
        // RESUME
        // =====================================================

        List<ResumeInterviewSession> resumeSessions =
                resumeSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                ResumeInterviewSession session :
                resumeSessions
        ) {

            if (
                    session == null
                            ||
                            !isResumeCompleted(session)
            ) {

                continue;
            }


            List<ResumeInterviewQuestion> questions =
                    resumeQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            for (
                    ResumeInterviewQuestion question :
                    questions
            ) {

                if (
                        question == null
                ) {

                    continue;
                }


                if (
                        question.getSkill() != null
                                &&
                                !question.getSkill().isBlank()
                                &&
                                question.getScore() != null
                ) {

                    addSkillScore(

                            typeSkillScores.get("RESUME"),

                            question.getSkill().trim(),

                            normalizeScore(
                                    question.getScore()
                            )
                    );
                }
            }
        }


        // =====================================================
        // CONVERT TO DTO
        // =====================================================

        Map<String, List<SkillPerformanceDTO>> result =
                new LinkedHashMap<>();


        for (
                Map.Entry<String, Map<String, List<Double>>> typeEntry :
                typeSkillScores.entrySet()
        ) {

            List<SkillPerformanceDTO> list =
                    new ArrayList<>();


            for (
                    Map.Entry<String, List<Double>> skillEntry :
                    typeEntry.getValue().entrySet()
            ) {

                double average =
                        skillEntry.getValue()

                                .stream()

                                .mapToDouble(
                                        Double::doubleValue
                                )

                                .average()

                                .orElse(0.0);


                list.add(

                        SkillPerformanceDTO.builder()

                                .name(
                                        skillEntry.getKey()
                                )

                                .value(
                                        round(
                                                average
                                        )
                                )

                                .build()
                );
            }


            list.sort(
                    Comparator.comparing(
                            SkillPerformanceDTO::getValue,
                            Comparator.reverseOrder()
                    )
            );


            if (
                    list.size() > 10
            ) {

                list =
                        new ArrayList<>(
                                list.subList(
                                        0,
                                        10
                                )
                        );
            }


            result.put(
                    typeEntry.getKey(),
                    list
            );
        }


        return result;
    }


    // =========================================================
    // ADD SKILL SCORE
    // =========================================================

    private void addSkillScore(
            Map<String, List<Double>> map,
            String skill,
            double score
    ) {

        if (
                map == null
                        ||
                        skill == null
                        ||
                        skill.isBlank()
        ) {

            return;
        }


        map.computeIfAbsent(
                skill.trim(),
                key ->
                        new ArrayList<>()
        ).add(
                clamp(
                        score,
                        0,
                        100
                )
        );
    }


    // =========================================================
    // TYPE-WISE STRENGTHS
    // =========================================================

    private Map<String, List<SkillPerformanceDTO>> buildTypeWiseStrengths(
            Map<String, List<SkillPerformanceDTO>> typeWiseSkills
    ) {

        Map<String, List<SkillPerformanceDTO>> result =
                new LinkedHashMap<>();


        for (
                String type :
                getSupportedTypes()
        ) {

            List<SkillPerformanceDTO> skills =
                    typeWiseSkills.getOrDefault(
                            type,
                            List.of()
                    );


            List<SkillPerformanceDTO> strengths =
                    skills.stream()

                            .filter(
                                    Objects::nonNull
                            )

                            .filter(
                                    item ->
                                            item.getValue() != null
                                                    &&
                                                    item.getValue() >= 80
                            )

                            .sorted(
                                    Comparator.comparing(
                                            SkillPerformanceDTO::getValue,
                                            Comparator.reverseOrder()
                                    )
                            )

                            .limit(5)

                            .toList();


            result.put(
                    type,
                    strengths
            );
        }


        return result;
    }


    // =========================================================
    // TYPE-WISE IMPROVEMENTS
    // =========================================================

    private Map<String, List<SkillPerformanceDTO>> buildTypeWiseImprovements(
            Map<String, List<SkillPerformanceDTO>> typeWiseSkills
    ) {

        Map<String, List<SkillPerformanceDTO>> result =
                new LinkedHashMap<>();


        for (
                String type :
                getSupportedTypes()
        ) {

            List<SkillPerformanceDTO> skills =
                    typeWiseSkills.getOrDefault(
                            type,
                            List.of()
                    );


            List<SkillPerformanceDTO> improvements =
                    skills.stream()

                            .filter(
                                    Objects::nonNull
                            )

                            .filter(
                                    item ->
                                            item.getValue() != null
                                                    &&
                                                    item.getValue() < 70
                            )

                            .sorted(
                                    Comparator.comparing(
                                            SkillPerformanceDTO::getValue
                                    )
                            )

                            .limit(5)

                            .toList();


            result.put(
                    type,
                    improvements
            );
        }


        return result;
    }


    // =========================================================
    // STREAK
    // =========================================================

    // =========================================================
// STREAK
// =========================================================

    private StreakDTO buildStreak(
            List<HistoryInterviewDTO> interviews
    ) {

        if (
                interviews == null ||
                        interviews.isEmpty()
        ) {

            return StreakDTO.builder()

                    .days(
                            0
                    )

                    .week(
                            buildCurrentWeek(
                                    Set.of()
                            )
                    )

                    .build();
        }


        // =====================================================
        // COLLECT ALL ACTIVE / COMPLETED DAYS
        // =====================================================

        Set<LocalDate> activeDays =
                interviews.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .filter(
                                item ->
                                        item.getCompletedAt() != null
                        )

                        .map(
                                item ->
                                        item.getCompletedAt()
                                                .toLocalDate()
                        )

                        .collect(
                                Collectors.toSet()
                        );


        // =====================================================
        // CURRENT STREAK
        // =====================================================

        int streak =
                calculateCurrentStreak(
                        activeDays
                );


        // =====================================================
        // CURRENT CALENDAR WEEK
        // MONDAY -> SUNDAY
        // =====================================================

        List<Boolean> week =
                buildCurrentWeek(
                        activeDays
                );


        return StreakDTO.builder()

                .days(
                        streak
                )

                .week(
                        week
                )

                .build();
    }


    // =========================================================
// BUILD CURRENT CALENDAR WEEK
//
// Monday -> Sunday
// =========================================================

    private List<Boolean> buildCurrentWeek(
            Set<LocalDate> activeDays
    ) {

        List<Boolean> week =
                new ArrayList<>();


        LocalDate today =
                LocalDate.now();


        /*
         * Java:
         *
         * Monday = 1
         * Tuesday = 2
         * ...
         * Sunday = 7
         *
         * Go backwards to Monday.
         */

        LocalDate monday =
                today.minusDays(
                        today.getDayOfWeek().getValue() - 1L
                );


        for (
                int i = 0;
                i < 7;
                i++
        ) {

            LocalDate date =
                    monday.plusDays(i);


            week.add(
                    activeDays.contains(
                            date
                    )
            );
        }


        return week;
    }


// =========================================================
// CURRENT STREAK
//
// Counts consecutive completed days ending today.
//
// Example:
//
// MON ✅
// TUE ✅
// WED ✅
// THU ❌
//
// Today = THU
//
// Streak = 0
//
// Example:
//
// MON ✅
// TUE ✅
// WED ✅
//
// Today = WED
//
// Streak = 3
// =========================================================

    private int calculateCurrentStreak(
            Set<LocalDate> activeDays
    ) {

        if (
                activeDays == null ||
                        activeDays.isEmpty()
        ) {

            return 0;
        }


        LocalDate today =
                LocalDate.now();


        /*
         * -----------------------------------------------------
         * IMPORTANT:
         *
         * Current streak normally means the consecutive
         * practice days ending TODAY.
         *
         * If the user has NOT practiced today,
         * streak becomes 0.
         *
         * -----------------------------------------------------
         */

        if (
                !activeDays.contains(
                        today
                )
        ) {

            return 0;
        }


        int streak =
                0;


        LocalDate currentDate =
                today;


        while (
                activeDays.contains(
                        currentDate
                )
        ) {

            streak++;


            currentDate =
                    currentDate.minusDays(1);
        }


        return streak;
    }

    // =========================================================
    // AI INSIGHT
    // =========================================================

    private AIInsightDTO buildInsight(
            List<HistoryInterviewDTO> interviews
    ) {

        List<HistoryInterviewDTO> sorted =
                interviews.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .filter(
                                item ->
                                        item.getCompletedAt() != null
                        )

                        .sorted(
                                Comparator.comparing(
                                        HistoryInterviewDTO::getCompletedAt,
                                        Comparator.reverseOrder()
                                )
                        )

                        .toList();


        if (
                sorted.size() < 2
        ) {

            return AIInsightDTO.builder()

                    .title(
                            "Keep building your interview streak"
                    )

                    .message(
                            "Complete more interviews to unlock personalized performance insights."
                    )

                    .build();
        }


        double recent =
                sorted.stream()

                        .limit(5)

                        .map(
                                HistoryInterviewDTO::getScore
                        )

                        .filter(
                                Objects::nonNull
                        )

                        .mapToDouble(
                                Double::doubleValue
                        )

                        .average()

                        .orElse(0.0);


        double previous =
                sorted.stream()

                        .skip(5)

                        .limit(5)

                        .map(
                                HistoryInterviewDTO::getScore
                        )

                        .filter(
                                Objects::nonNull
                        )

                        .mapToDouble(
                                Double::doubleValue
                        )

                        .average()

                        .orElse(recent);


        if (
                recent > previous + 5
        ) {

            return AIInsightDTO.builder()

                    .title(
                            "Your interview confidence is improving"
                    )

                    .message(
                            "Your recent interview scores are higher than your previous sessions."
                    )

                    .build();
        }


        if (
                recent < previous - 5
        ) {

            return AIInsightDTO.builder()

                    .title(
                            "A little more focused practice can help"
                    )

                    .message(
                            "Your recent scores have dipped. Focus on the skills where your answers are weakest."
                    )

                    .build();
        }


        return AIInsightDTO.builder()

                .title(
                        "Your performance is staying consistent"
                )

                .message(
                        "Your recent scores are relatively stable. Keep practicing to push your average higher."
                )

                .build();
    }


    // =========================================================
    // MONTHLY CHANGE
    // =========================================================

    private double calculateMonthlyChange(
            List<HistoryInterviewDTO> interviews
    ) {

        if (
                interviews == null
                        ||
                        interviews.isEmpty()
        ) {

            return 0.0;
        }


        YearMonth currentMonth =
                YearMonth.now();


        YearMonth previousMonth =
                currentMonth.minusMonths(1);


        double currentAverage =
                interviews.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .filter(
                                item ->
                                        item.getCompletedAt() != null
                                                &&
                                                YearMonth.from(
                                                        item.getCompletedAt()
                                                ).equals(
                                                        currentMonth
                                                )
                        )

                        .map(
                                HistoryInterviewDTO::getScore
                        )

                        .filter(
                                Objects::nonNull
                        )

                        .mapToDouble(
                                Double::doubleValue
                        )

                        .average()

                        .orElse(0.0);


        double previousAverage =
                interviews.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .filter(
                                item ->
                                        item.getCompletedAt() != null
                                                &&
                                                YearMonth.from(
                                                        item.getCompletedAt()
                                                ).equals(
                                                        previousMonth
                                                )
                        )

                        .map(
                                HistoryInterviewDTO::getScore
                        )

                        .filter(
                                Objects::nonNull
                        )

                        .mapToDouble(
                                Double::doubleValue
                        )

                        .average()

                        .orElse(0.0);


        if (
                previousAverage == 0
        ) {

            return 0.0;
        }


        return (
                (
                        currentAverage -
                                previousAverage
                )
                        /
                        previousAverage
        ) * 100.0;
    }


    // =========================================================
    // STREAK CALCULATION
    // =========================================================

    // =========================================================
// STREAK CALCULATION
// =========================================================

    private int calculateStreakDays(
            List<HistoryInterviewDTO> interviews
    ) {

        if (
                interviews == null ||
                        interviews.isEmpty()
        ) {

            return 0;
        }


        Set<LocalDate> dates =
                interviews.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .filter(
                                item ->
                                        item.getCompletedAt() != null
                        )

                        .map(
                                item ->
                                        item.getCompletedAt()
                                                .toLocalDate()
                        )

                        .collect(
                                Collectors.toSet()
                        );


        if (
                dates.isEmpty()
        ) {

            return 0;
        }


        LocalDate today =
                LocalDate.now();


        /*
         * Current streak must include TODAY.
         *
         * If user has not completed an interview today,
         * current streak is 0.
         */

        if (
                !dates.contains(
                        today
                )
        ) {

            return 0;
        }


        int streak =
                0;


        while (
                dates.contains(
                        today
                )
        ) {

            streak++;


            today =
                    today.minusDays(1);
        }


        return streak;
    }

    // =========================================================
    // SCORE TAG
    // =========================================================

    private String scoreTag(
            double score
    ) {

        double safeScore =
                clamp(
                        score,
                        0,
                        100
                );


        if (
                safeScore >= 90
        ) {

            return "Outstanding";
        }


        if (
                safeScore >= 80
        ) {

            return "Excellent";
        }


        if (
                safeScore >= 70
        ) {

            return "Good";
        }


        if (
                safeScore >= 60
        ) {

            return "Improving";
        }


        return "Needs Work";
    }


    // =========================================================
    // DURATION
    // =========================================================

    private int durationSeconds(
            LocalDateTime start,
            LocalDateTime end
    ) {

        if (
                start == null
                        ||
                        end == null
                        ||
                        end.isBefore(start)
        ) {

            return 0;
        }


        long seconds =
                ChronoUnit.SECONDS.between(
                        start,
                        end
                );


        if (
                seconds <= 0
        ) {

            return 0;
        }


        return seconds > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) seconds;
    }


    // =========================================================
    // FORMAT DURATION
    // =========================================================

    private String formatDuration(
            long seconds
    ) {

        if (
                seconds <= 0
        ) {

            return "0m";
        }


        long hours =
                seconds / 3600;


        long minutes =
                (
                        seconds % 3600
                ) / 60;


        if (
                hours > 0
        ) {

            return hours +
                    "h " +
                    minutes +
                    "m";
        }


        if (
                minutes > 0
        ) {

            return minutes +
                    "m";
        }


        return Math.max(
                1,
                seconds
        ) + "s";
    }


    // =========================================================
    // VOICE COMPLETION
    // =========================================================

    private boolean isVoiceCompleted(
            VoiceInterviewSession session
    ) {

        return session != null
                &&
                (
                        Boolean.TRUE.equals(
                                session.getCompleted()
                        )
                                ||
                                session.getCompletedAt() != null
                );
    }


    // =========================================================
    // RESUME COMPLETION
    // =========================================================

    private boolean isResumeCompleted(
            ResumeInterviewSession session
    ) {

        return session != null
                &&
                (
                        Boolean.TRUE.equals(
                                session.getCompleted()
                        )
                                ||
                                session.getCompletedAt() != null
                );
    }


    // =========================================================
    // QUICK DRILL COMPLETION
    // =========================================================

    private boolean isQuickDrillCompleted(
            QuickDrillSession session
    ) {

        return session != null
                &&
                (
                        Boolean.TRUE.equals(
                                session.getCompleted()
                        )
                                ||
                                session.getCompletedAt() != null
                );
    }


    // =========================================================
    // VOICE FALLBACK SCORE
    // =========================================================

    private Double safeVoiceScore(
            VoiceInterviewSession session
    ) {

        if (
                session == null
                        ||
                        session.getTotalScore() == null
        ) {

            return 0.0;
        }


        return session
                .getTotalScore()
                .doubleValue();
    }


    // =========================================================
    // QUICK DRILL FALLBACK SCORE
    // =========================================================

    private Double safeQuickScore(
            QuickDrillSession session
    ) {

        if (
                session == null
                        ||
                        session.getTotalScore() == null
        ) {

            return 0.0;
        }


        double totalScore =
                session
                        .getTotalScore()
                        .doubleValue();


        if (
                session.getTotalQuestions() != null
                        &&
                        session.getTotalQuestions() > 0
        ) {

            return totalScore /
                    session.getTotalQuestions()
                            .doubleValue();
        }


        return totalScore;
    }


    // =========================================================
    // NORMALIZE SCORE
    // =========================================================

    private double normalizeScore(
            Double score
    ) {

        if (
                score == null
                        ||
                        score.isNaN()
                        ||
                        score.isInfinite()
        ) {

            return 0.0;
        }


        double value =
                score.doubleValue();


        /*
         * 0-10 -> 0-100
         */

        if (
                value >= 0
                        &&
                        value <= 10
        ) {

            value *= 10.0;
        }


        return round(
                clamp(
                        value,
                        0,
                        100
                )
        );
    }


    // =========================================================
    // CLAMP
    // =========================================================

    private double clamp(
            double value,
            double min,
            double max
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
    // ROUND
    // =========================================================

    private double round(
            double value
    ) {

        if (
                Double.isNaN(value)
                        ||
                        Double.isInfinite(value)
        ) {

            return 0.0;
        }


        return Math.round(
                value * 100.0
        ) / 100.0;
    }


    // =========================================================
    // COMPLETED CHECK
    // =========================================================

    private boolean isCompleted(
            HistoryInterviewDTO item
    ) {

        return item != null
                &&
                item.getCompletedAt() != null;
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safeString(
            String value,
            String fallback
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return fallback;
        }


        return value.trim();
    }


    // =========================================================
    // READ JSON STRING LIST
    // =========================================================

    private List<String> readStringList(
            String json
    ) {

        if (
                json == null
                        ||
                        json.isBlank()
        ) {

            return new ArrayList<>();
        }


        try {

            List<String> result =
                    objectMapper.readValue(
                            json,
                            new TypeReference<List<String>>() {}
                    );


            if (
                    result == null
            ) {

                return new ArrayList<>();
            }


            return result.stream()

                    .filter(
                            item ->
                                    item != null
                                            &&
                                            !item.isBlank()
                    )

                    .map(
                            String::trim
                    )

                    .toList();

        } catch (
                Exception e
        ) {

            System.err.println(
                    "❌ FAILED TO PARSE JSON LIST: "
                            + e.getMessage()
            );

            return new ArrayList<>();
        }
    }


    // =========================================================
    // HISTORY DETAIL
    // =========================================================

    public HistoryDetailResponse getHistoryDetail(
            String type,
            Long id
    ) {

        if (
                type == null
                        ||
                        type.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Interview type is required"
            );
        }


        if (
                id == null
        ) {

            throw new IllegalArgumentException(
                    "Interview id is required"
            );
        }


        String normalizedType =
                type.trim()
                        .toUpperCase();


        switch (
                normalizedType
        ) {

            case "VOICE":

                return getVoiceHistoryDetail(
                        id
                );


            case "RESUME":

                return getResumeHistoryDetail(
                        id
                );


            case "QUICK_DRILL":

                return getQuickDrillHistoryDetail(
                        id
                );


            case "COMMUNICATION":

                return getConversationHistoryDetail(
                        id,
                        false
                );


            case "HR":

                return getConversationHistoryDetail(
                        id,
                        true
                );


            default:

                throw new IllegalArgumentException(
                        "Unsupported interview type: " +
                                type
                );
        }
    }


    // =========================================================
    // SCORE TO 100
    // =========================================================

    private double normalize100(
            double score
    ) {

        if (
                score > 10
        ) {

            return clamp(
                    score,
                    0,
                    100
            );
        }


        return clamp(
                score * 10,
                0,
                100
        );
    }


    private double toPercentageScore(
            double score
    ) {

        return normalize100(
                score
        );
    }


    // =========================================================
    // SCORE TAG 100
    // =========================================================

    private String scoreTag100(
            double score
    ) {

        if (
                score >= 90
        ) {

            return "Outstanding";
        }


        if (
                score >= 80
        ) {

            return "Excellent";
        }


        if (
                score >= 70
        ) {

            return "Good";
        }


        if (
                score >= 60
        ) {

            return "Improving";
        }


        return "Needs Work";
    }


    // =========================================================
    // VOICE FEEDBACK
    // =========================================================

    private String buildVoiceFeedback(
            double score
    ) {

        double percentage =
                normalize100(
                        score
                );


        if (
                percentage >= 90
        ) {

            return "Excellent interview performance. Keep maintaining this level.";
        }


        if (
                percentage >= 80
        ) {

            return "Very good performance. Focus on consistency and deeper answers.";
        }


        if (
                percentage >= 70
        ) {

            return "Good performance. A little more focused practice can improve your score.";
        }


        if (
                percentage >= 60
        ) {

            return "You are improving. Work on clarity, structure and technical depth.";
        }


        return "More practice is recommended. Review your weak areas and retry the interview.";
    }


    // =========================================================
    // VOICE DETAIL
    // =========================================================

    private HistoryDetailResponse getVoiceHistoryDetail(
            Long sessionId
    ) {

        VoiceInterviewSession session =
                voiceSessionRepository.findById(
                        sessionId
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Voice interview session not found: " +
                                                sessionId
                                )
                );


        List<VoiceInterviewQuestion> questions =
                voiceQuestionRepository
                        .findBySessionIdOrderByQuestionNumberAsc(
                                sessionId
                        );


        double score =
                questions.stream()

                        .filter(
                                q ->
                                        q != null
                                                &&
                                                q.getScore() != null
                        )

                        .mapToDouble(
                                q ->
                                        q.getScore()
                        )

                        .average()

                        .orElse(
                                session.getTotalScore() == null
                                        ? 0
                                        : session.getTotalScore()
                        );


        int answered =
                (int)
                        questions.stream()

                                .filter(
                                        q ->
                                                q != null
                                                        &&
                                                        q.getAnswer() != null
                                                        &&
                                                        !q.getAnswer().isBlank()
                                )

                                .count();


        List<HistoryQuestionDTO> questionDTOs =
                questions.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .map(
                                q ->
                                        HistoryQuestionDTO.builder()

                                                .questionNumber(
                                                        q.getQuestionNumber()
                                                )

                                                .question(
                                                        q.getQuestion()
                                                )

                                                .userAnswer(
                                                        q.getAnswer()
                                                )

                                                .score(
                                                        q.getScore() == null
                                                                ? 0.0
                                                                : normalize100(
                                                                q.getScore()
                                                        )
                                                )

                                                .feedback(
                                                        q.getFeedback()
                                                )

                                                .skill(
                                                        q.getSkill()
                                                )

                                                .evaluated(
                                                        q.getScore() != null
                                                )

                                                .build()
                        )

                        .toList();


        Map<String, Double> skillMap =
                questions.stream()

                        .filter(
                                q ->
                                        q != null
                                                &&
                                                q.getSkill() != null
                                                &&
                                                !q.getSkill().isBlank()
                                                &&
                                                q.getScore() != null
                        )

                        .collect(
                                Collectors.groupingBy(
                                        q ->
                                                q.getSkill().trim(),

                                        LinkedHashMap::new,

                                        Collectors.averagingDouble(
                                                q ->
                                                        q.getScore()
                                        )
                                )
                        );


        List<HistorySkillDTO> skills =
                skillMap.entrySet()

                        .stream()

                        .map(
                                entry ->
                                        HistorySkillDTO.builder()

                                                .name(
                                                        entry.getKey()
                                                )

                                                .value(
                                                        round(
                                                                normalize100(
                                                                        entry.getValue()
                                                                )
                                                        )
                                                )

                                                .build()
                        )

                        .toList();


        List<String> strengths =
                new ArrayList<>();


        List<String> improvements =
                new ArrayList<>();


        double communication =
                averageVoiceMetric(
                        questions,
                        VoiceMetric.COMMUNICATION
                );


        double technical =
                averageVoiceMetric(
                        questions,
                        VoiceMetric.TECHNICAL
                );


        if (
                communication >= 70
        ) {

            strengths.add(
                    "Good communication performance"
            );

        } else {

            improvements.add(
                    "Improve communication and answer structure"
            );
        }


        if (
                technical >= 70
        ) {

            strengths.add(
                    "Strong technical knowledge"
            );

        } else {

            improvements.add(
                    "Improve technical knowledge"
            );
        }


        return HistoryDetailResponse.builder()

                .id(
                        sessionId
                )

                .title(
                        safeString(
                                session.getSelectedRole(),
                                "Voice Interview"
                        )
                )

                .type(
                        "VOICE"
                )

                .score(
                        round(
                                normalize100(
                                        score
                                )
                        )
                )

                .tag(
                        scoreTag100(
                                normalize100(
                                        score
                                )
                        )
                )

                .completedAt(
                        session.getCompletedAt()
                )

                .durationSeconds(
                        durationSeconds(
                                session.getStartedAt(),
                                session.getCompletedAt()
                        )
                )

                .totalQuestions(
                        questions.size()
                )

                .answeredQuestions(
                        answered
                )

                .questions(
                        questionDTOs
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

                .finalFeedback(
                        buildVoiceFeedback(
                                score
                        )
                )

                .build();
    }


    // =========================================================
    // RESUME DETAIL
    // =========================================================

    private HistoryDetailResponse getResumeHistoryDetail(
            Long sessionId
    ) {

        ResumeInterviewSession session =
                resumeSessionRepository.findById(
                        sessionId
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Resume interview session not found: " +
                                                sessionId
                                )
                );


        ResumeInterviewAnalysis analysis =
                null;


        if (
                session.getAnalysisId() != null
        ) {

            analysis =
                    resumeAnalysisRepository.findById(
                            session.getAnalysisId()
                    ).orElse(null);
        }


        List<ResumeInterviewQuestion> resumeQuestions =
                resumeQuestionRepository
                        .findBySessionIdOrderByQuestionNumberAsc(
                                sessionId
                        );


        double score =
                resumeQuestions.stream()

                        .filter(
                                question ->
                                        question != null
                                                &&
                                                question.getScore() != null
                        )

                        .mapToDouble(
                                question ->
                                        normalizeScore(
                                                question.getScore()
                                        )
                        )

                        .average()

                        .orElse(
                                analysis != null
                                        &&
                                        analysis.getResumeScore() != null
                                        ? normalizeScore(
                                        analysis.getResumeScore()
                                                .doubleValue()
                                )
                                        : 0.0
                        );


        List<HistoryQuestionDTO> questionDTOs =
                new ArrayList<>();


        for (
                ResumeInterviewQuestion question :
                resumeQuestions
        ) {

            if (
                    question == null
            ) {

                continue;
            }


            questionDTOs.add(

                    HistoryQuestionDTO.builder()

                            .questionNumber(
                                    question.getQuestionNumber()
                            )

                            .question(
                                    question.getQuestion()
                            )

                            .userAnswer(
                                    question.getAnswer()
                            )

                            .score(
                                    question.getScore() == null
                                            ? 0.0
                                            : normalizeScore(
                                            question.getScore()
                                    )
                            )

                            .feedback(
                                    question.getFeedback()
                            )

                            .skill(
                                    question.getSkill()
                            )

                            .evaluated(
                                    question.getScore() != null
                            )

                            .build()
            );
        }


        int answeredQuestions =
                (int)
                        resumeQuestions.stream()

                                .filter(
                                        question ->
                                                question != null
                                                        &&
                                                        question.getAnswer() != null
                                                        &&
                                                        !question
                                                                .getAnswer()
                                                                .isBlank()
                                )

                                .count();


        int totalQuestions;


        if (
                !resumeQuestions.isEmpty()
        ) {

            totalQuestions =
                    resumeQuestions.size();

        } else if (
                session.getTotalQuestions() != null
        ) {

            totalQuestions =
                    session.getTotalQuestions();

        } else {

            totalQuestions =
                    session.getCurrentQuestionNumber() == null
                            ? 0
                            : session.getCurrentQuestionNumber();
        }


        Map<String, List<Double>> skillScoreMap =
                new LinkedHashMap<>();


        for (
                ResumeInterviewQuestion question :
                resumeQuestions
        ) {

            if (
                    question == null
                            ||
                            question.getSkill() == null
                            ||
                            question.getSkill().isBlank()
                            ||
                            question.getScore() == null
            ) {

                continue;
            }


            addSkillScore(

                    skillScoreMap,

                    question.getSkill().trim(),

                    normalizeScore(
                            question.getScore()
                    )
            );
        }


        List<HistorySkillDTO> skills =
                new ArrayList<>();


        for (
                Map.Entry<String, List<Double>> entry :
                skillScoreMap.entrySet()
        ) {

            skills.add(

                    HistorySkillDTO.builder()

                            .name(
                                    entry.getKey()
                            )

                            .value(
                                    round(
                                            averageScores(
                                                    entry.getValue()
                                            )
                                    )
                            )

                            .build()
            );
        }


        skills.sort(
                Comparator.comparing(
                        HistorySkillDTO::getValue,
                        Comparator.reverseOrder()
                )
        );


        if (
                skills.isEmpty()
                        &&
                        analysis != null
        ) {

            List<String> analysisSkills =
                    readStringList(
                            analysis.getSkillsJson()
                    );


            for (
                    String skill :
                    analysisSkills
            ) {

                skills.add(

                        HistorySkillDTO.builder()

                                .name(
                                        skill
                                )

                                .value(
                                        round(
                                                score
                                        )
                                )

                                .build()
                );
            }
        }


        List<String> strengths =
                analysis == null
                        ? new ArrayList<>()
                        : readStringList(
                        analysis.getStrengthsJson()
                );


        List<String> improvements =
                analysis == null
                        ? new ArrayList<>()
                        : readStringList(
                        analysis.getImprovementsJson()
                );


        String finalFeedback =
                "Resume based interview completed.";


        if (
                analysis != null
                        &&
                        analysis.getOverallSummary() != null
                        &&
                        !analysis.getOverallSummary().isBlank()
        ) {

            finalFeedback =
                    analysis.getOverallSummary();
        }


        return HistoryDetailResponse.builder()

                .id(
                        sessionId
                )

                .title(
                        "Resume Interview"
                )

                .type(
                        "RESUME"
                )

                .score(
                        round(
                                score
                        )
                )

                .tag(
                        scoreTag100(
                                score
                        )
                )

                .completedAt(
                        session.getCompletedAt()
                )

                .durationSeconds(
                        durationSeconds(
                                session.getStartedAt(),
                                session.getCompletedAt()
                        )
                )

                .totalQuestions(
                        totalQuestions
                )

                .answeredQuestions(
                        answeredQuestions
                )

                .questions(
                        questionDTOs
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

                .finalFeedback(
                        finalFeedback
                )

                .build();
    }


    // =========================================================
    // QUICK DRILL DETAIL
    // =========================================================

    private HistoryDetailResponse getQuickDrillHistoryDetail(
            Long drillId
    ) {

        QuickDrillSession session =
                quickDrillSessionRepository.findById(
                        drillId
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Quick drill not found: " +
                                                drillId
                                )
                );


        List<QuickDrillQuestion> questions =
                quickDrillQuestionRepository
                        .findByDrillIdOrderByQuestionNumberAsc(
                                drillId
                        );


        double averageScore =
                questions.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .map(
                                QuickDrillQuestion::getScore
                        )

                        .filter(
                                Objects::nonNull
                        )

                        .mapToDouble(
                                Double::doubleValue
                        )

                        .average()

                        .orElse(0.0);


        List<HistoryQuestionDTO> questionDTOs =
                questions.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .map(
                                q ->
                                        HistoryQuestionDTO.builder()

                                                .questionNumber(
                                                        q.getQuestionNumber()
                                                )

                                                .question(
                                                        q.getQuestion()
                                                )

                                                .userAnswer(
                                                        q.getUserAnswer()
                                                )

                                                .score(
                                                        q.getScore() == null
                                                                ? 0.0
                                                                : normalize100(
                                                                q.getScore()
                                                        )
                                                )

                                                .difficulty(
                                                        q.getDifficulty()
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


        List<HistorySkillDTO> skills =
                questions.stream()

                        .filter(
                                q ->
                                        q != null
                                                &&
                                                q.getTopic() != null
                                                &&
                                                !q.getTopic().isBlank()
                                                &&
                                                q.getScore() != null
                        )

                        .collect(
                                Collectors.groupingBy(
                                        q ->
                                                q.getTopic().trim(),

                                        LinkedHashMap::new,

                                        Collectors.averagingDouble(
                                                QuickDrillQuestion::getScore
                                        )
                                )
                        )

                        .entrySet()

                        .stream()

                        .map(
                                entry ->
                                        HistorySkillDTO.builder()

                                                .name(
                                                        entry.getKey()
                                                )

                                                .value(
                                                        round(
                                                                normalize100(
                                                                        entry.getValue()
                                                                )
                                                        )
                                                )

                                                .build()
                        )

                        .toList();


        return HistoryDetailResponse.builder()

                .id(
                        drillId
                )

                .title(
                        "Quick Drill"
                )

                .type(
                        "QUICK_DRILL"
                )

                .score(
                        round(
                                normalize100(
                                        averageScore
                                )
                        )
                )

                .tag(
                        scoreTag100(
                                normalize100(
                                        averageScore
                                )
                        )
                )

                .completedAt(
                        session.getCompletedAt()
                )

                .durationSeconds(
                        durationSeconds(
                                session.getStartedAt(),
                                session.getCompletedAt()
                        )
                )

                .totalQuestions(
                        session.getTotalQuestions()
                )

                .answeredQuestions(
                        session.getAnsweredQuestions()
                )

                .questions(
                        questionDTOs
                )

                .skills(
                        skills
                )

                .strengths(
                        new ArrayList<>()
                )

                .improvements(
                        new ArrayList<>()
                )

                .finalFeedback(
                        "Quick drill performance completed."
                )

                .build();
    }


    // =========================================================
    // COMMUNICATION / HR DETAIL
    // =========================================================

    private HistoryDetailResponse getConversationHistoryDetail(
            Long sessionId,
            boolean hr
    ) {

        ConversationSession session =
                conversationSessionRepository.findById(
                        sessionId
                ).orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Conversation session not found: " +
                                                sessionId
                                )
                );


        List<ConversationMessage> messages =
                conversationMessageRepositroy
                        .findBySessionIdOrderByCreatedAtAsc(
                                sessionId
                        );


        List<HistoryQuestionDTO> questionDTOs =
                new ArrayList<>();


        int questionNumber =
                0;


        for (
                ConversationMessage message :
                messages
        ) {

            if (
                    message == null
                            ||
                            message.getSender() == null
            ) {

                continue;
            }


            String sender =
                    message.getSender()
                            .name();


            if (
                    "AI".equalsIgnoreCase(
                            sender
                    )
            ) {

                questionNumber++;


                questionDTOs.add(

                        HistoryQuestionDTO.builder()

                                .questionNumber(
                                        questionNumber
                                )

                                .question(
                                        message.getMessage()
                                )

                                .build()
                );


                continue;
            }


            if (
                    "USER".equalsIgnoreCase(
                            sender
                    )
            ) {

                if (
                        !questionDTOs.isEmpty()
                ) {

                    HistoryQuestionDTO last =
                            questionDTOs.get(
                                    questionDTOs.size() - 1
                            );


                    last.setUserAnswer(
                            message.getMessage()
                    );


                    if (
                            message.getScore() != null
                    ) {

                        last.setScore(
                                normalize100(
                                        message.getScore()
                                )
                        );
                    }


                    last.setFeedback(
                            message.getFeedback()
                    );


                    last.setEvaluated(
                            message.getScore() != null
                    );
                }
            }
        }


        /*
         * IMPORTANT:
         *
         * ConversationSession stores the actual final
         * overall score after interview completion.
         */

        double overallScore =
                normalizeScore(
                        session.getOverallScore()
                );


        return HistoryDetailResponse.builder()

                .id(
                        sessionId
                )

                .title(
                        hr
                                ? "HR Interview"
                                : "Communication Interview"
                )

                .type(
                        hr
                                ? "HR"
                                : "COMMUNICATION"
                )

                .score(
                        round(
                                overallScore
                        )
                )

                .tag(
                        scoreTag100(
                                overallScore
                        )
                )

                .completedAt(
                        session.getEndedAt()
                )

                .durationSeconds(
                        durationSeconds(
                                session.getStartedAt(),
                                session.getEndedAt()
                        )
                )

                .totalQuestions(
                        questionNumber
                )

                .answeredQuestions(
                        (int)
                                questionDTOs.stream()

                                        .filter(
                                                q ->
                                                        q.getUserAnswer() != null
                                                                &&
                                                                !q.getUserAnswer().isBlank()
                                        )

                                        .count()
                )

                .questions(
                        questionDTOs
                )

                /*
                 * Actual metric scores are stored at session level.
                 *
                 * HistorySkillDTO supports name/value, so we expose
                 * the five actual dimensions here.
                 */

                .skills(
                        buildConversationDetailSkills(
                                session
                        )
                )

                .strengths(
                        buildConversationStrengths(
                                session
                        )
                )

                .improvements(
                        buildConversationImprovements(
                                session
                        )
                )

                .finalFeedback(
                        safeString(
                                session.getFinalFeedback(),
                                "Interview completed."
                        )
                )

                .build();
    }


    // =========================================================
    // CONVERSATION DETAIL SKILLS
    // =========================================================

    private List<HistorySkillDTO> buildConversationDetailSkills(
            ConversationSession session
    ) {

        List<HistorySkillDTO> skills =
                new ArrayList<>();


        if (
                session == null
        ) {

            return skills;
        }


        if (
                session.getTechnicalScore() != null
        ) {

            skills.add(

                    HistorySkillDTO.builder()

                            .name(
                                    "Technical"
                            )

                            .value(
                                    round(
                                            normalizeScore(
                                                    session.getTechnicalScore()
                                            )
                                    )
                            )

                            .build()
            );
        }


        if (
                session.getCommunicationScore() != null
        ) {

            skills.add(

                    HistorySkillDTO.builder()

                            .name(
                                    "Communication"
                            )

                            .value(
                                    round(
                                            normalizeScore(
                                                    session.getCommunicationScore()
                                            )
                                    )
                            )

                            .build()
            );
        }


        if (
                session.getProblemSolvingScore() != null
        ) {

            skills.add(

                    HistorySkillDTO.builder()

                            .name(
                                    "Problem Solving"
                            )

                            .value(
                                    round(
                                            normalizeScore(
                                                    session.getProblemSolvingScore()
                                            )
                                    )
                            )

                            .build()
            );
        }


        if (
                session.getConfidenceScore() != null
        ) {

            skills.add(

                    HistorySkillDTO.builder()

                            .name(
                                    "Confidence"
                            )

                            .value(
                                    round(
                                            normalizeScore(
                                                    session.getConfidenceScore()
                                            )
                                    )
                            )

                            .build()
            );
        }


        return skills;
    }


    // =========================================================
    // CONVERSATION STRENGTHS
    // =========================================================

    private List<String> buildConversationStrengths(
            ConversationSession session
    ) {

        List<String> result =
                new ArrayList<>();


        if (
                session == null
        ) {

            return result;
        }


        if (
                session.getTechnicalScore() != null
                        &&
                        normalizeScore(
                                session.getTechnicalScore()
                        ) >= 80
        ) {

            result.add(
                    "Strong technical performance"
            );
        }


        if (
                session.getCommunicationScore() != null
                        &&
                        normalizeScore(
                                session.getCommunicationScore()
                        ) >= 80
        ) {

            result.add(
                    "Strong communication"
            );
        }


        if (
                session.getProblemSolvingScore() != null
                        &&
                        normalizeScore(
                                session.getProblemSolvingScore()
                        ) >= 80
        ) {

            result.add(
                    "Strong problem-solving ability"
            );
        }


        if (
                session.getConfidenceScore() != null
                        &&
                        normalizeScore(
                                session.getConfidenceScore()
                        ) >= 80
        ) {

            result.add(
                    "Good confidence and clarity"
            );
        }


        return result;
    }


    // =========================================================
    // CONVERSATION IMPROVEMENTS
    // =========================================================

    private List<String> buildConversationImprovements(
            ConversationSession session
    ) {

        List<String> result =
                new ArrayList<>();


        if (
                session == null
        ) {

            return result;
        }


        if (
                session.getTechnicalScore() != null
                        &&
                        normalizeScore(
                                session.getTechnicalScore()
                        ) < 70
        ) {

            result.add(
                    "Improve technical understanding"
            );
        }


        if (
                session.getCommunicationScore() != null
                        &&
                        normalizeScore(
                                session.getCommunicationScore()
                        ) < 70
        ) {

            result.add(
                    "Improve communication and clarity"
            );
        }


        if (
                session.getProblemSolvingScore() != null
                        &&
                        normalizeScore(
                                session.getProblemSolvingScore()
                        ) < 70
        ) {

            result.add(
                    "Improve problem-solving and reasoning"
            );
        }


        if (
                session.getConfidenceScore() != null
                        &&
                        normalizeScore(
                                session.getConfidenceScore()
                        ) < 70
        ) {

            result.add(
                    "Improve confidence and answer completeness"
            );
        }


        return result;
    }


    // =========================================================
    // READINESS
    // =========================================================

    public ReadinessResponse getReadiness(
            Long userId
    ) {

        if (
                userId == null
        ) {

            throw new IllegalArgumentException(
                    "userId is required"
            );
        }


        // =====================================================
        // OVERALL
        // =====================================================

        double overall =
                calculateReadinessOverall(
                        userId
                );


        // =====================================================
        // TECHNICAL
        // =====================================================

        double technical =
                calculateTechnicalReadiness(
                        userId
                );


        // =====================================================
        // COMMUNICATION
        // =====================================================

        double communication =
                calculateCommunicationReadiness(
                        userId
                );


        // =====================================================
        // PROBLEM SOLVING
        // =====================================================

        double problemSolving =
                calculateProblemSolvingReadiness(
                        userId
                );


        // =====================================================
        // CONFIDENCE
        // =====================================================

        double confidence =
                calculateConfidenceReadiness(
                        userId
                );


        return ReadinessResponse.builder()

                .overallScore(
                        round(
                                overall
                        )
                )

                .technical(
                        round(
                                technical
                        )
                )

                .communication(
                        round(
                                communication
                        )
                )

                .problemSolving(
                        round(
                                problemSolving
                        )
                )

                .confidence(
                        round(
                                confidence
                        )
                )

                .build();
    }


    // =========================================================
    // OVERALL READINESS
    //
    // Each completed interview gets equal weight.
    // =========================================================

    private double calculateReadinessOverall(
            Long userId
    ) {

        List<Double> interviewScores =
                new ArrayList<>();


        List<VoiceInterviewSession> voiceSessions =
                voiceSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                VoiceInterviewSession session :
                voiceSessions
        ) {

            if (
                    session == null
                            ||
                            !isVoiceCompleted(session)
            ) {

                continue;
            }


            List<VoiceInterviewQuestion> questions =
                    voiceQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            double score =
                    questions.stream()

                            .filter(
                                    q ->
                                            q != null
                                                    &&
                                                    q.getScore() != null
                            )

                            .mapToDouble(
                                    q ->
                                            normalizeScore(
                                                    q.getScore().doubleValue()
                                            )
                            )

                            .average()

                            .orElse(
                                    safeVoiceScore(
                                            session
                                    )
                            );


            interviewScores.add(
                    score
            );
        }


        // =====================================================
        // RESUME
        // =====================================================

        List<ResumeInterviewSession> resumeSessions =
                resumeSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                ResumeInterviewSession session :
                resumeSessions
        ) {

            if (
                    session == null
                            ||
                            !isResumeCompleted(session)
            ) {

                continue;
            }


            List<ResumeInterviewQuestion> questions =
                    resumeQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            double score =
                    questions.stream()

                            .filter(
                                    q ->
                                            q != null
                                                    &&
                                                    q.getScore() != null
                            )

                            .mapToDouble(
                                    q ->
                                            normalizeScore(
                                                    q.getScore()
                                            )
                            )

                            .average()

                            .orElseGet(
                                    () ->
                                            getResumeAnalysisScore(
                                                    session
                                            )
                            );


            interviewScores.add(
                    score
            );
        }


        // =====================================================
        // QUICK DRILL
        // =====================================================

        List<QuickDrillSession> drills =
                quickDrillSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                QuickDrillSession drill :
                drills
        ) {

            if (
                    drill == null
                            ||
                            !isQuickDrillCompleted(drill)
            ) {

                continue;
            }


            interviewScores.add(
                    normalizeScore(
                            safeQuickScore(
                                    drill
                            )
                    )
            );
        }


        // =====================================================
        // COMMUNICATION + HR
        // =====================================================

        List<ConversationSession> conversations =
                conversationSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                ConversationSession session :
                conversations
        ) {

            if (
                    session == null
                            ||
                            session.getEndedAt() == null
            ) {

                continue;
            }


            if (
                    session.getOverallScore() != null
            ) {

                interviewScores.add(
                        normalizeScore(
                                session.getOverallScore()
                        )
                );
            }
        }


        return averageScores(
                interviewScores
        );
    }


    // =========================================================
    // TECHNICAL READINESS
    //
    // Voice  -> technicalKnowledgeScore
    // Resume -> technicalScore
    // HR     -> session technicalScore
    // Communication -> session technicalScore
    //
    // Quick Drill intentionally excluded because its entity
    // has no dedicated technical score.
    // =========================================================

    private double calculateTechnicalReadiness(
            Long userId
    ) {

        List<Double> interviewScores =
                new ArrayList<>();


        // =====================================================
        // VOICE
        // =====================================================

        List<VoiceInterviewSession> voiceSessions =
                voiceSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                VoiceInterviewSession session :
                voiceSessions
        ) {

            if (
                    session == null
                            ||
                            !isVoiceCompleted(session)
            ) {

                continue;
            }


            List<VoiceInterviewQuestion> questions =
                    voiceQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            List<Double> questionScores =
                    new ArrayList<>();


            for (
                    VoiceInterviewQuestion question :
                    questions
            ) {

                if (
                        question != null
                                &&
                                question.getTechnicalKnowledgeScore() != null
                ) {

                    questionScores.add(
                            normalizeIntegerScore(
                                    question
                                            .getTechnicalKnowledgeScore()
                            )
                    );
                }
            }


            addInterviewAverage(
                    interviewScores,
                    questionScores
            );
        }


        // =====================================================
        // RESUME
        // =====================================================

        List<ResumeInterviewSession> resumeSessions =
                resumeSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                ResumeInterviewSession session :
                resumeSessions
        ) {

            if (
                    session == null
                            ||
                            !isResumeCompleted(session)
            ) {

                continue;
            }


            List<ResumeInterviewQuestion> questions =
                    resumeQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            List<Double> questionScores =
                    new ArrayList<>();


            for (
                    ResumeInterviewQuestion question :
                    questions
            ) {

                if (
                        question != null
                                &&
                                question.getTechnicalScore() != null
                ) {

                    questionScores.add(
                            normalizeScore(
                                    question.getTechnicalScore()
                            )
                    );
                }
            }


            addInterviewAverage(
                    interviewScores,
                    questionScores
            );
        }


        // =====================================================
        // HR + COMMUNICATION
        // =====================================================

        addConversationMetric(
                userId,
                interviewScores,
                ConversationMetric.TECHNICAL
        );


        return averageScores(
                interviewScores
        );
    }


    // =========================================================
    // COMMUNICATION READINESS
    //
    // Voice  -> communicationScore
    // Resume -> communicationScore
    // HR/Communication -> session communicationScore
    // =========================================================

    private double calculateCommunicationReadiness(
            Long userId
    ) {

        List<Double> interviewScores =
                new ArrayList<>();


        // =====================================================
        // VOICE
        // =====================================================

        List<VoiceInterviewSession> voiceSessions =
                voiceSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                VoiceInterviewSession session :
                voiceSessions
        ) {

            if (
                    session == null
                            ||
                            !isVoiceCompleted(session)
            ) {

                continue;
            }


            List<VoiceInterviewQuestion> questions =
                    voiceQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            List<Double> questionScores =
                    new ArrayList<>();


            for (
                    VoiceInterviewQuestion question :
                    questions
            ) {

                if (
                        question != null
                                &&
                                question.getCommunicationScore() != null
                ) {

                    questionScores.add(
                            normalizeIntegerScore(
                                    question.getCommunicationScore()
                            )
                    );
                }
            }


            addInterviewAverage(
                    interviewScores,
                    questionScores
            );
        }


        // =====================================================
        // RESUME
        // =====================================================

        List<ResumeInterviewSession> resumeSessions =
                resumeSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                ResumeInterviewSession session :
                resumeSessions
        ) {

            if (
                    session == null
                            ||
                            !isResumeCompleted(session)
            ) {

                continue;
            }


            List<ResumeInterviewQuestion> questions =
                    resumeQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            List<Double> questionScores =
                    new ArrayList<>();


            for (
                    ResumeInterviewQuestion question :
                    questions
            ) {

                if (
                        question != null
                                &&
                                question.getCommunicationScore() != null
                ) {

                    questionScores.add(
                            normalizeScore(
                                    question.getCommunicationScore()
                            )
                    );
                }
            }


            addInterviewAverage(
                    interviewScores,
                    questionScores
            );
        }


        // =====================================================
        // HR + COMMUNICATION
        // =====================================================

        addConversationMetric(
                userId,
                interviewScores,
                ConversationMetric.COMMUNICATION
        );


        return averageScores(
                interviewScores
        );
    }


    // =========================================================
    // PROBLEM SOLVING READINESS
    //
    // Voice  -> problemSolvingScore
    // Resume -> problemSolvingScore
    // HR/Communication -> session problemSolvingScore
    // =========================================================

    private double calculateProblemSolvingReadiness(
            Long userId
    ) {

        List<Double> interviewScores =
                new ArrayList<>();


        // =====================================================
        // VOICE
        // =====================================================

        List<VoiceInterviewSession> voiceSessions =
                voiceSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                VoiceInterviewSession session :
                voiceSessions
        ) {

            if (
                    session == null
                            ||
                            !isVoiceCompleted(session)
            ) {

                continue;
            }


            List<VoiceInterviewQuestion> questions =
                    voiceQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            List<Double> questionScores =
                    new ArrayList<>();


            for (
                    VoiceInterviewQuestion question :
                    questions
            ) {

                if (
                        question != null
                                &&
                                question.getProblemSolvingScore() != null
                ) {

                    questionScores.add(
                            normalizeIntegerScore(
                                    question
                                            .getProblemSolvingScore()
                            )
                    );
                }
            }


            addInterviewAverage(
                    interviewScores,
                    questionScores
            );
        }


        // =====================================================
        // RESUME
        // =====================================================

        List<ResumeInterviewSession> resumeSessions =
                resumeSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                ResumeInterviewSession session :
                resumeSessions
        ) {

            if (
                    session == null
                            ||
                            !isResumeCompleted(session)
            ) {

                continue;
            }


            List<ResumeInterviewQuestion> questions =
                    resumeQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            List<Double> questionScores =
                    new ArrayList<>();


            for (
                    ResumeInterviewQuestion question :
                    questions
            ) {

                if (
                        question != null
                                &&
                                question.getProblemSolvingScore() != null
                ) {

                    questionScores.add(
                            normalizeScore(
                                    question.getProblemSolvingScore()
                            )
                    );
                }
            }


            addInterviewAverage(
                    interviewScores,
                    questionScores
            );
        }


        // =====================================================
        // HR + COMMUNICATION
        // =====================================================

        addConversationMetric(
                userId,
                interviewScores,
                ConversationMetric.PROBLEM_SOLVING
        );


        return averageScores(
                interviewScores
        );
    }


    // =========================================================
    // CONFIDENCE READINESS
    //
    // Voice  -> confidenceClarityScore
    // Resume -> confidenceScore
    // HR/Communication -> session confidenceScore
    // =========================================================

    private double calculateConfidenceReadiness(
            Long userId
    ) {

        List<Double> interviewScores =
                new ArrayList<>();


        // =====================================================
        // VOICE
        // =====================================================

        List<VoiceInterviewSession> voiceSessions =
                voiceSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                VoiceInterviewSession session :
                voiceSessions
        ) {

            if (
                    session == null
                            ||
                            !isVoiceCompleted(session)
            ) {

                continue;
            }


            List<VoiceInterviewQuestion> questions =
                    voiceQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            List<Double> questionScores =
                    new ArrayList<>();


            for (
                    VoiceInterviewQuestion question :
                    questions
            ) {

                if (
                        question != null
                                &&
                                question.getConfidenceClarityScore() != null
                ) {

                    questionScores.add(
                            normalizeIntegerScore(
                                    question
                                            .getConfidenceClarityScore()
                            )
                    );
                }
            }


            addInterviewAverage(
                    interviewScores,
                    questionScores
            );
        }


        // =====================================================
        // RESUME
        // =====================================================

        List<ResumeInterviewSession> resumeSessions =
                resumeSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                ResumeInterviewSession session :
                resumeSessions
        ) {

            if (
                    session == null
                            ||
                            !isResumeCompleted(session)
            ) {

                continue;
            }


            List<ResumeInterviewQuestion> questions =
                    resumeQuestionRepository
                            .findBySessionIdOrderByQuestionNumberAsc(
                                    session.getSessionId()
                            );


            List<Double> questionScores =
                    new ArrayList<>();


            for (
                    ResumeInterviewQuestion question :
                    questions
            ) {

                if (
                        question != null
                                &&
                                question.getConfidenceScore() != null
                ) {

                    questionScores.add(
                            normalizeScore(
                                    question.getConfidenceScore()
                            )
                    );
                }
            }


            addInterviewAverage(
                    interviewScores,
                    questionScores
            );
        }


        // =====================================================
        // HR + COMMUNICATION
        // =====================================================

        addConversationMetric(
                userId,
                interviewScores,
                ConversationMetric.CONFIDENCE
        );


        return averageScores(
                interviewScores
        );
    }


    // =========================================================
    // ADD CONVERSATION METRIC
    // =========================================================

    private void addConversationMetric(

            Long userId,

            List<Double> interviewScores,

            ConversationMetric metric

    ) {

        List<ConversationSession> sessions =
                conversationSessionRepository
                        .findByUserIdOrderByStartedAtDesc(
                                userId
                        );


        for (
                ConversationSession session :
                sessions
        ) {

            if (
                    session == null
                            ||
                            session.getEndedAt() == null
            ) {

                continue;
            }


            Double score =
                    null;


            switch (
                    metric
            ) {

                case TECHNICAL:

                    score =
                            session.getTechnicalScore();

                    break;


                case COMMUNICATION:

                    score =
                            session.getCommunicationScore();

                    break;


                case PROBLEM_SOLVING:

                    score =
                            session.getProblemSolvingScore();

                    break;


                case CONFIDENCE:

                    score =
                            session.getConfidenceScore();

                    break;
            }


            if (
                    score != null
            ) {

                interviewScores.add(
                        normalizeScore(
                                score
                        )
                );
            }
        }
    }


    // =========================================================
    // ADD INTERVIEW AVERAGE
    // =========================================================

    private void addInterviewAverage(

            List<Double> interviewScores,

            List<Double> questionScores

    ) {

        if (
                questionScores == null
                        ||
                        questionScores.isEmpty()
        ) {

            return;
        }


        double average =
                questionScores.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .mapToDouble(
                                Double::doubleValue
                        )

                        .average()

                        .orElse(-1.0);


        if (
                average >= 0
        ) {

            interviewScores.add(
                    clamp(
                            average,
                            0,
                            100
                    )
            );
        }
    }


    // =========================================================
    // AVERAGE SCORES
    // =========================================================

    private double averageScores(
            List<Double> scores
    ) {

        if (
                scores == null
                        ||
                        scores.isEmpty()
        ) {

            return 0.0;
        }


        return round(

                scores.stream()

                        .filter(
                                Objects::nonNull
                        )

                        .filter(
                                value ->
                                        !value.isNaN()
                                                &&
                                                !value.isInfinite()
                        )

                        .mapToDouble(
                                value ->
                                        clamp(
                                                value,
                                                0,
                                                100
                                        )
                        )

                        .average()

                        .orElse(0.0)
        );
    }


    // =========================================================
    // NORMALIZE INTEGER SCORE
    // =========================================================

    private double normalizeIntegerScore(
            Integer score
    ) {

        if (
                score == null
        ) {

            return 0.0;
        }


        double value =
                score.doubleValue();


        if (
                value >= 0
                        &&
                        value <= 10
        ) {

            value *= 10.0;
        }


        return clamp(
                value,
                0,
                100
        );
    }


    // =========================================================
    // AVERAGE VOICE METRIC
    // =========================================================

    private double averageVoiceMetric(

            List<VoiceInterviewQuestion> questions,

            VoiceMetric metric

    ) {

        List<Double> scores =
                new ArrayList<>();


        for (
                VoiceInterviewQuestion question :
                questions
        ) {

            if (
                    question == null
            ) {

                continue;
            }


            Integer value =
                    null;


            switch (
                    metric
            ) {

                case TECHNICAL:

                    value =
                            question
                                    .getTechnicalKnowledgeScore();

                    break;


                case COMMUNICATION:

                    value =
                            question
                                    .getCommunicationScore();

                    break;
            }


            if (
                    value != null
            ) {

                scores.add(
                        normalizeIntegerScore(
                                value
                        )
                );
            }
        }


        return averageScores(
                scores
        );
    }


    // =========================================================
    // GET RESUME ANALYSIS SCORE
    // =========================================================

    private double getResumeAnalysisScore(
            ResumeInterviewSession session
    ) {

        if (
                session == null
                        ||
                        session.getAnalysisId() == null
        ) {

            return 0.0;
        }


        ResumeInterviewAnalysis analysis =
                resumeAnalysisRepository
                        .findById(
                                session.getAnalysisId()
                        )
                        .orElse(null);


        if (
                analysis == null
                        ||
                        analysis.getResumeScore() == null
        ) {

            return 0.0;
        }


        return normalizeScore(
                analysis.getResumeScore().doubleValue()
        );
    }


    // =========================================================
    // VOICE METRIC
    // =========================================================

    private enum VoiceMetric {

        TECHNICAL,

        COMMUNICATION
    }


    // =========================================================
    // CONVERSATION METRIC
    // =========================================================

    private enum ConversationMetric {

        TECHNICAL,

        COMMUNICATION,

        PROBLEM_SOLVING,

        CONFIDENCE
    }
}