package com.example.MyFirstApp.service.HRandCommunication;

import com.example.MyFirstApp.DTO.HRandCommunication.EndInterviewResponse;

import com.example.MyFirstApp.Entity.HRandCoummunicationEntity.ConversationMessage;
import com.example.MyFirstApp.Entity.HRandCoummunicationEntity.ConversationSession;

import com.example.MyFirstApp.Enum.InterviewMode;
import com.example.MyFirstApp.Enum.MessageSender;
import com.example.MyFirstApp.Enum.SessionStatus;

import com.example.MyFirstApp.repository.HRandCoummunicationRespository.ConversationMessageRepositroy;
import com.example.MyFirstApp.repository.HRandCoummunicationRespository.ConversationSessionRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.Disposable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ConversationInterviewService {

    // =========================================================
    // REPOSITORIES
    // =========================================================

    private final ConversationSessionRepository sessionRepository;

    private final ConversationMessageRepositroy messageRepository;


    // =========================================================
    // AI SERVICES
    // =========================================================

    private final ConversationGroqService groqService;

    private final ConversationGroqTTService groqTTService;


    // =========================================================
    // OBJECT MAPPER
    // =========================================================

    private final ObjectMapper objectMapper =
            new ObjectMapper();


    // =========================================================
    // ACTIVE STREAMS
    // =========================================================

    private final Map<Long, Disposable>
            activeProcesses =
            new ConcurrentHashMap<>();


    // =========================================================
    // START CONVERSATION
    // =========================================================

    public Disposable startConversation(

            Long sessionId,

            BiConsumer<String, String> onAudioReady,

            Runnable onComplete,

            Consumer<Throwable> onError

    ) {

        if (sessionId == null) {

            if (onError != null) {

                onError.accept(
                        new IllegalArgumentException(
                                "sessionId cannot be null"
                        )
                );
            }

            return noOp();
        }


        ConversationSession session =
                sessionRepository
                        .findById(sessionId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Conversation session not found"
                                        )
                        );


        // =====================================================
        // BUILD NEW OPENING QUESTION
        // =====================================================

        String firstMessage =
                buildFirstMessage(
                        session.getMode(),
                        sessionId
                );


        // =====================================================
        // SAVE AI MESSAGE
        // =====================================================

        saveAiMessage(
                sessionId,
                firstMessage
        );


        // =====================================================
        // TTS
        // =====================================================

        Disposable disposable =
                groqTTService.generateSpeechChunks(

                        firstMessage,

                        onAudioReady,

                        onComplete,

                        onError
                );


        activeProcesses.put(
                sessionId,
                disposable
        );


        return disposable;
    }


    // =========================================================
    // FIRST MESSAGE
    //
    // IMPORTANT:
    // Do NOT always start with "Tell me about yourself".
    // Different realistic opening questions are used.
    // =========================================================

    private String buildFirstMessage(

            InterviewMode mode,

            Long sessionId

    ) {

        // =====================================================
        // HR INTERVIEW QUESTIONS
        // =====================================================

        if (mode == InterviewMode.HR) {

            List<String> hrQuestions =
                    List.of(

                            "Hello Adarsh. Welcome to the HR interview. Let's start with a simple question. What interested you in applying for this role?",

                            "Hello Adarsh. Let's begin the interview. What are you currently working on, and what kind of opportunity are you looking for next?",

                            "Hello Adarsh. Let's start. Why do you think you are a good fit for this position?",

                            "Hello Adarsh. Before we discuss your technical background, tell me about one professional achievement that you are particularly proud of.",

                            "Hello Adarsh. Let's begin with your career goals. Where do you see yourself professionally over the next two to three years?",

                            "Hello Adarsh. Imagine you receive two job offers with similar responsibilities. What factors would you consider before choosing one?",

                            "Hello Adarsh. Let's start with something practical. What are your salary expectations for this role?",

                            "Hello Adarsh. Tell me about a situation where you had to work with someone whose working style was very different from yours.",

                            "Hello Adarsh. Suppose you join our company and your manager gives you a task with a deadline that you believe is unrealistic. How would you handle the situation?",

                            "Hello Adarsh. Imagine you are made the team leader of a team where two members are constantly disagreeing. What would you do?",

                            "Hello Adarsh. Why are you considering a job change, or why are you looking for a new opportunity?",

                            "Hello Adarsh. Suppose another company offers you a higher salary after you join us. How would you handle that situation?",

                            "Hello Adarsh. Tell me about one weakness you are currently working to improve.",

                            "Hello Adarsh. Suppose your manager gives you negative feedback that you strongly disagree with. How would you respond?",

                            "Hello Adarsh. Imagine your team misses an important deadline because one team member made a mistake. As the team leader, what would you do?",

                            "Hello Adarsh. If you were given responsibility for a project but had very little information about how to start it, what would your first few steps be?"

                    );


            int index =
                    Math.floorMod(
                            sessionId.hashCode(),
                            hrQuestions.size()
                    );


            return hrQuestions.get(index);
        }


        // =====================================================
        // COMMUNICATION / ENGLISH PRACTICE QUESTIONS
        // =====================================================

        List<String> communicationQuestions =
                List.of(

                        "Hello Adarsh. Let's practice your spoken English through a natural conversation. What did you do today?",

                        "Hello Adarsh. Let's start with something simple. Tell me about a project you are currently working on.",

                        "Hello Adarsh. Imagine you meet a new colleague on your first day at work. How would you introduce yourself and start a conversation?",

                        "Hello Adarsh. Tell me about something you learned recently and explain why you found it interesting.",

                        "Hello Adarsh. Imagine you have to explain your favorite technology to someone who has never used it. How would you explain it?",

                        "Hello Adarsh. Tell me about a difficult problem you faced recently and how you handled it.",

                        "Hello Adarsh. Imagine you are working with a team and you disagree with another team member's idea. How would you express your opinion politely?",

                        "Hello Adarsh. Suppose your manager asks you to explain a technical problem to a non-technical person. How would you explain it?",

                        "Hello Adarsh. Let's talk about your future goals. What kind of professional life do you want to build?",

                        "Hello Adarsh. Imagine you are meeting an interviewer for the first time. How would you start the conversation?",

                        "Hello Adarsh. Tell me about a recent situation where you had to make an important decision.",

                        "Hello Adarsh. Imagine you have to convince your friend or teammate to try a technology you like. How would you convince them?",

                        "Hello Adarsh. Tell me about a mistake you made while learning something and what you learned from it.",

                        "Hello Adarsh. Imagine you are leading a small team. How would you motivate the team when everyone is feeling stressed?",

                        "Hello Adarsh. Let's have a casual conversation. What do you usually do when you have free time?",

                        "Hello Adarsh. Tell me about one skill you want to improve this year and why."

                );


        int index =
                Math.floorMod(
                        sessionId.hashCode(),
                        communicationQuestions.size()
                );


        return communicationQuestions.get(index);
    }


    // =========================================================
    // SAVE USER MESSAGE
    // =========================================================

    private ConversationMessage saveUserMessage(

            Long sessionId,

            String text,

            EvaluationResult evaluation

    ) {

        ConversationMessage message =
                ConversationMessage.builder()

                        .sessionId(
                                sessionId
                        )

                        .sender(
                                MessageSender.USER
                        )

                        .message(
                                text
                        )

                        .score(
                                evaluation == null
                                        ? null
                                        : evaluation.score()
                        )

                        .technicalScore(
                                evaluation == null
                                        ? null
                                        : evaluation.technicalScore()
                        )

                        .communicationScore(
                                evaluation == null
                                        ? null
                                        : evaluation.communicationScore()
                        )

                        .problemSolvingScore(
                                evaluation == null
                                        ? null
                                        : evaluation.problemSolvingScore()
                        )

                        .confidenceScore(
                                evaluation == null
                                        ? null
                                        : evaluation.confidenceScore()
                        )

                        .feedback(
                                evaluation == null
                                        ? null
                                        : evaluation.feedback()
                        )

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .build();


        return messageRepository.save(
                message
        );
    }


    // =========================================================
    // SAVE AI MESSAGE
    // =========================================================

    private ConversationMessage saveAiMessage(

            Long sessionId,

            String text

    ) {

        ConversationMessage message =
                ConversationMessage.builder()

                        .sessionId(
                                sessionId
                        )

                        .sender(
                                MessageSender.AI
                        )

                        .message(
                                text
                        )

                        .score(
                                null
                        )

                        .technicalScore(
                                null
                        )

                        .communicationScore(
                                null
                        )

                        .problemSolvingScore(
                                null
                        )

                        .confidenceScore(
                                null
                        )

                        .feedback(
                                null
                        )

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .build();


        return messageRepository.save(
                message
        );
    }


    // =========================================================
    // BUILD HISTORY
    // =========================================================

    private String buildConversationHistory(
            Long sessionId
    ) {

        List<ConversationMessage> messages =
                messageRepository
                        .findBySessionIdOrderByCreatedAtAsc(
                                sessionId
                        );


        StringBuilder history =
                new StringBuilder();


        for (
                ConversationMessage message :
                messages
        ) {

            if (
                    message == null
                            ||
                            message.getMessage() == null
            ) {

                continue;
            }


            if (
                    message.getSender()
                            == MessageSender.USER
            ) {

                history.append(
                        "USER: "
                );

            } else {

                history.append(
                        "AI: "
                );
            }


            history.append(
                    message.getMessage()
            );


            history.append(
                    "\n"
            );
        }


        return history.toString();
    }


    // =========================================================
    // COMMUNICATION / ENGLISH PRACTICE PROMPT
    // =========================================================

    private String buildCommunicationPrompt(

            String history,

            String userAnswer

    ) {

        return """
                You are a professional English communication interviewer
                and spoken-English coach.

                You are conducting a REALISTIC interactive English
                conversation practice session.

                Your job is NOT simply to ask random English questions.

                Your job is to:

                1. Listen carefully to the candidate's answer.
                2. Understand what the candidate is trying to say.
                3. Evaluate the English naturally.
                4. Identify important grammar, vocabulary or sentence
                   formation mistakes.
                5. Correct mistakes briefly and clearly.
                6. Continue the conversation naturally.
                7. Ask exactly ONE relevant follow-up question.
                8. Make the next question depend on the candidate's answer.
                9. Never repeatedly ask the same question.
                10. Make the conversation feel like a real person is talking
                    to the candidate.

                =========================================================
                IMPORTANT: DO NOT BLINDLY ACCEPT WRONG ANSWERS
                =========================================================

                If the candidate gives an incorrect, unclear or weak answer,
                do NOT simply say:

                "Good answer."

                and move to another unrelated question.

                Instead:

                - Recognize what the candidate was trying to say.
                - Clearly identify the important problem.
                - Give a short correction or better way to say it.
                - Continue the conversation.
                - Ask a logical follow-up question.

                Example:

                Candidate:
                "I am working in this company since two years."

                Good response style:

                "I understand what you mean. A more natural way to say that is:
                'I have been working at this company for two years.'
                What has been the most challenging part of your work so far?"

                Do NOT turn the conversation into a grammar lecture.


                =========================================================
                ENGLISH CORRECTION RULE
                =========================================================

                If the candidate makes a meaningful English mistake,
                briefly correct it.

                Use natural phrases such as:

                "A more natural way to say that is..."

                "A better way to say this would be..."

                "Small correction..."

                Do NOT correct every tiny mistake.

                Focus on mistakes that affect:

                - Meaning
                - Grammar
                - Sentence structure
                - Natural spoken English
                - Professional communication

                The conversation must continue after the correction.


                =========================================================
                NATURAL CONVERSATION
                =========================================================

                Do NOT ask disconnected questions.

                Use the candidate's previous answer to create
                the next question.

                Example:

                Candidate:
                "I recently worked on a Spring Boot project."

                Next question:

                "That sounds interesting. What was the most difficult
                part of that project?"

                Then if the candidate says:

                "Database was difficult because I don't know PostgreSQL."

                Next question could be:

                "What did you do when you got stuck with PostgreSQL?"

                The conversation should feel connected.


                =========================================================
                ENGLISH LEVEL
                =========================================================

                Adapt your language to the candidate.

                If the candidate's English is weak:

                - Use simple English.
                - Ask manageable questions.
                - Correct important mistakes.
                - Gradually increase difficulty.

                If the candidate is comfortable:

                - Use more natural conversational English.
                - Ask longer questions.
                - Introduce professional vocabulary.
                - Ask opinion-based and situational questions.

                If the candidate becomes stronger during the conversation,
                gradually increase the difficulty.


                =========================================================
                QUESTION TYPES
                =========================================================

                Do not ask only:

                "Tell me about yourself."

                Use different conversation topics such as:

                - Daily life
                - Studies
                - Work
                - Projects
                - Technology
                - Hobbies
                - Opinions
                - Problem solving
                - Workplace situations
                - Teamwork
                - Leadership
                - Future plans
                - Professional communication
                - Situational questions

                Questions should become more interesting as
                the conversation progresses.


                =========================================================
                SCORING
                =========================================================

                Score the candidate's LATEST answer from 0 to 100.

                Evaluate:

                technicalScore:
                Relevant knowledge or understanding demonstrated.
                For a non-technical question, score relevance and
                practical understanding.

                communicationScore:
                Grammar, vocabulary, fluency, clarity,
                sentence structure and natural English.

                problemSolvingScore:
                Logical thinking, reasoning and ability to explain ideas.

                confidenceScore:
                Confidence, decisiveness, completeness and clarity.

                score:
                Overall quality of the latest response.

                Score honestly.

                Do not give high scores simply because the candidate
                attempted an answer.


                =========================================================
                FEEDBACK
                =========================================================

                Feedback should be short and useful.

                Example:

                "Your idea was clear, but your sentence structure needs improvement. Try using complete sentences and connecting your ideas more naturally."

                Do not write a long evaluation.

                The detailed conversation should happen naturally
                through the interviewer reply.


                =========================================================
                RESPONSE FORMAT
                =========================================================

                Return ONLY valid JSON.

                Do NOT use markdown.

                Do NOT use code fences.

                Do NOT write anything before or after JSON.

                Required structure:

                {
                  "reply": "Natural interviewer response, including a brief correction if needed, followed by exactly ONE relevant question.",
                  "score": 0,
                  "technicalScore": 0,
                  "communicationScore": 0,
                  "problemSolvingScore": 0,
                  "confidenceScore": 0,
                  "feedback": "Short practical feedback."
                }


                =========================================================
                VERY IMPORTANT QUESTION RULE
                =========================================================

                Ask EXACTLY ONE question in "reply".

                Never ask two questions.

                Never ask a question that was already asked earlier
                in the conversation history.

                The question must be relevant to the candidate's
                latest answer.

                Do not finish the interview.

                Continue the conversation.


                =========================================================
                CONVERSATION HISTORY
                =========================================================

                """
                + history
                +
                """

                =========================================================
                LATEST CANDIDATE ANSWER
                =========================================================

                """
                + userAnswer
                +
                """

                =========================================================
                FINAL TASK
                =========================================================

                Evaluate the latest answer honestly.

                If there is an important English mistake,
                correct it briefly.

                If the answer is weak or incorrect,
                do not pretend that it is correct.

                Then continue the conversation naturally.

                Ask exactly ONE logical follow-up question.

                Return JSON only.
                """;
    }


    // =========================================================
    // HR INTERVIEW PROMPT
    // =========================================================

    private String buildHrPrompt(

            String history,

            String userAnswer

    ) {

        return """
                You are a SENIOR PROFESSIONAL HR INTERVIEWER.

                You are conducting a realistic job interview with a candidate.

                Your behavior must be similar to an experienced human HR
                interviewer, NOT a friendly chatbot.

                =========================================================
                PRIMARY OBJECTIVE
                =========================================================

                Evaluate how the candidate thinks, communicates,
                handles situations, explains decisions and behaves
                in a professional environment.

                You must actively listen to the candidate's answer.

                Do not simply ask a sequence of unrelated predefined questions.

                Every next question should be based on:

                - The candidate's latest answer
                - Previous conversation
                - The candidate's claims
                - Weak points in the answer
                - Contradictions
                - Missing information
                - Real workplace situations


                =========================================================
                DO NOT BLINDLY ACCEPT WRONG ANSWERS
                =========================================================

                This is extremely important.

                If the candidate gives a weak, unrealistic,
                incomplete or incorrect answer:

                DO NOT say:

                "That's a good answer."

                just to continue the interview.

                Instead:

                1. Acknowledge the candidate's point if appropriate.
                2. Identify the concern or missing point.
                3. Briefly challenge the candidate.
                4. Ask a logical follow-up question.

                Example:

                Candidate:
                "If my team member makes a mistake, I will blame him
                because he should be responsible."

                Better interviewer behavior:

                "I understand that accountability is important, but as a
                team leader, blaming someone immediately can make the
                situation worse. How would you handle the mistake while
                still making sure the team member takes responsibility?"

                This is what a real interviewer should do.


                =========================================================
                TRICKY / LOGICAL HR QUESTIONS
                =========================================================

                Ask realistic HR questions such as:

                - Salary expectation
                - Salary negotiation
                - Why should we hire you?
                - Why are you leaving your current role?
                - Why do you want this company?
                - What if another company offers you more money?
                - What is your weakness?
                - What if your manager disagrees with you?
                - What if your teammate is not cooperating?
                - What if you become a team leader?
                - What if your team misses an important deadline?
                - How would you handle workplace conflict?
                - How would you handle pressure?
                - What if you receive negative feedback?
                - What if you make a serious mistake?
                - What if your manager gives you an unrealistic deadline?
                - What if you disagree with your senior?
                - How do you prioritize multiple urgent tasks?
                - How would you motivate a weak team member?
                - What would you do if a teammate takes credit for your work?
                - What would you do if you were asked to do something
                  you believe is wrong?
                - What matters more: salary, learning or growth?
                - Where do you see yourself in three to five years?
                - Why should we offer you the salary you expect?


                =========================================================
                SALARY CONVERSATION
                =========================================================

                Salary discussions should feel realistic.

                Do not immediately accept the candidate's salary expectation.

                Example flow:

                HR:
                "What are your salary expectations?"

                Candidate:
                "I expect 8 LPA."

                HR may respond:

                "What makes you believe 8 LPA is appropriate for your
                current experience and skill set?"

                Later:

                "Suppose our budget is 6 LPA. Would you be willing to
                consider it?"

                Then:

                "What factors would influence your decision besides salary?"

                Use realistic negotiation logic.


                =========================================================
                FOLLOW-UP QUESTIONS
                =========================================================

                Follow-up questions are extremely important.

                If the candidate says:

                "I am a good team player."

                Do NOT immediately move to another topic.

                Ask:

                "Can you give me a specific example of a situation where
                you worked effectively with a difficult team member?"

                If the candidate gives a generic answer,
                challenge it further.

                Example:

                "You mentioned that you communicate well. Can you describe
                a situation where your communication actually solved a
                workplace problem?"


                =========================================================
                CANDIDATE CONTRADICTIONS
                =========================================================

                If the candidate contradicts something said earlier,
                notice it.

                Politely ask about it.

                Example:

                "Earlier you mentioned that salary was not your main
                priority, but now you are saying compensation is the most
                important factor. How would you explain that?"

                Do not ignore contradictions.


                =========================================================
                REAL HR BEHAVIOR
                =========================================================

                You are professional.

                Do not praise every answer.

                Do not use excessive encouragement.

                Do not behave like a teacher during the interview.

                You may briefly acknowledge an answer:

                "I understand."

                "That's interesting."

                "Let's explore that."

                "I see your point."

                But then continue with a meaningful question.


                =========================================================
                ENGLISH COMMUNICATION
                =========================================================

                Even though this is an HR interview,
                the candidate's English communication should also be evaluated.

                If the candidate makes an important English mistake,
                you may briefly correct it.

                Example:

                Candidate:
                "I am working in this company since two years."

                Interviewer:

                "Small correction: a more natural way to say that is,
                'I have been working at this company for two years.'
                Now, coming back to your experience, what has been the
                biggest challenge you have faced there?"

                Do NOT turn the HR interview into a grammar lesson.

                Correct only important mistakes.

                Continue the interview immediately.


                =========================================================
                ANSWER QUALITY
                =========================================================

                Evaluate whether the candidate's answer is:

                - Relevant
                - Logical
                - Specific
                - Realistic
                - Professional
                - Structured
                - Confident
                - Consistent

                If the answer is generic,
                ask for a specific example.

                If the answer is unrealistic,
                challenge the candidate.

                If the answer is incomplete,
                ask about the missing part.

                If the answer is strong,
                increase the difficulty.


                =========================================================
                SCORING
                =========================================================

                Score the LATEST candidate answer from 0 to 100.

                technicalScore:
                Relevant technical/professional understanding.

                communicationScore:
                English, clarity, vocabulary, sentence structure,
                professionalism and ability to communicate.

                problemSolvingScore:
                Reasoning, decision-making, practical thinking,
                situation handling and logic.

                confidenceScore:
                Confidence, ownership, decisiveness and completeness.

                score:
                Overall answer quality.

                Score honestly.

                Do NOT inflate scores simply because the candidate
                attempted an answer.


                =========================================================
                SCORING GUIDELINES
                =========================================================

                90-100:
                Excellent, specific, logical and professional.

                80-89:
                Very strong answer with minor improvements needed.

                70-79:
                Good answer but some gaps exist.

                60-69:
                Average answer that needs improvement.

                40-59:
                Weak, generic or incomplete answer.

                0-39:
                Very weak, irrelevant, incorrect or poorly communicated.


                =========================================================
                FEEDBACK
                =========================================================

                Give short practical feedback.

                Example:

                "Your answer showed good ownership, but it was too general. In a real interview, support your answer with a specific example."

                Do not write a long evaluation.


                =========================================================
                RESPONSE FORMAT
                =========================================================

                Return ONLY valid JSON.

                No markdown.

                No code fences.

                No extra text.

                Required structure:

                {
                  "reply": "Natural professional HR interviewer response followed by exactly ONE logical follow-up question.",
                  "score": 0,
                  "technicalScore": 0,
                  "communicationScore": 0,
                  "problemSolvingScore": 0,
                  "confidenceScore": 0,
                  "feedback": "Short practical feedback."
                }


                =========================================================
                EXACTLY ONE QUESTION
                =========================================================

                The "reply" MUST contain exactly ONE question.

                Never ask multiple questions.

                Never ask a question that was already asked.

                Never ask an unrelated question.

                Always make the next question logically connected
                to the candidate's latest answer.

                Never end the interview yourself.


                =========================================================
                CONVERSATION HISTORY
                =========================================================

                """
                + history
                +
                """

                =========================================================
                LATEST CANDIDATE ANSWER
                =========================================================

                """
                + userAnswer
                +
                """

                =========================================================
                FINAL TASK
                =========================================================

                Act like a real experienced HR interviewer.

                Listen carefully.

                Evaluate honestly.

                If the answer is weak, challenge it.

                If the answer is incorrect, do not pretend it is correct.

                If the candidate makes an important English mistake,
                briefly correct it.

                Then continue the interview.

                Ask exactly ONE logical follow-up question.

                Return JSON only.
                """;
    }


    // =========================================================
    // PROCESS USER MESSAGE
    // =========================================================

    public Disposable processUserMessage(

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

            return fail(
                    onError,
                    new IllegalArgumentException(
                            "sessionId cannot be null"
                    )
            );
        }


        if (
                userAnswer == null
                        ||
                        userAnswer.isBlank()
        ) {

            return fail(
                    onError,
                    new IllegalArgumentException(
                            "User answer cannot be empty"
                    )
            );
        }


        // =====================================================
        // LOAD SESSION
        // =====================================================

        ConversationSession session =
                sessionRepository
                        .findById(
                                sessionId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Conversation session not found"
                                        )
                        );


        // =====================================================
        // COMPLETED CHECK
        // =====================================================

        if (
                session.getStatus()
                        == SessionStatus.COMPLETED
        ) {

            return fail(
                    onError,
                    new RuntimeException(
                            "Interview is already completed"
                    )
            );
        }


        String cleanedUserAnswer =
                userAnswer.trim();


        // =====================================================
        // BUILD HISTORY
        //
        // Latest user answer is NOT included yet.
        // =====================================================

        String history =
                buildConversationHistory(
                        sessionId
                );


        // =====================================================
        // BUILD PROMPT
        // =====================================================

        String prompt;

        if (
                session.getMode()
                        == InterviewMode.HR
        ) {

            prompt =
                    buildHrPrompt(
                            history,
                            cleanedUserAnswer
                    );

        } else {

            prompt =
                    buildCommunicationPrompt(
                            history,
                            cleanedUserAnswer
                    );
        }


        // =====================================================
        // LOG
        // =====================================================

        System.out.println(
                "========================================"
        );

        System.out.println(
                "🤖 CONVERSATION AI PROCESSING"
        );

        System.out.println(
                "SESSION ID: "
                        + sessionId
        );

        System.out.println(
                "MODE: "
                        + session.getMode()
        );

        System.out.println(
                "USER ANSWER: "
                        + cleanedUserAnswer
        );

        System.out.println(
                "========================================"
        );


        // =====================================================
        // AI RESPONSE BUFFER
        // =====================================================

        StringBuilder aiResponse =
                new StringBuilder();


        // =====================================================
        // GROQ STREAM
        // =====================================================

        Disposable disposable =
                groqService.streamAI(

                        prompt,

                        // =====================================
                        // AI CHUNK
                        // =====================================

                        chunk -> {

                            if (
                                    chunk == null
                                            ||
                                            chunk.isBlank()
                            ) {

                                return;
                            }


                            aiResponse.append(
                                    chunk
                            );

                        },


                        // =====================================
                        // AI COMPLETE
                        // =====================================

                        () -> {

                            try {

                                String rawResponse =
                                        aiResponse
                                                .toString()
                                                .trim();


                                if (
                                        rawResponse.isBlank()
                                ) {

                                    throw new RuntimeException(
                                            "Empty AI response"
                                    );
                                }


                                System.out.println(
                                        "========================================"
                                );

                                System.out.println(
                                        "✅ RAW CONVERSATION AI RESPONSE"
                                );

                                System.out.println(
                                        rawResponse
                                );

                                System.out.println(
                                        "========================================"
                                );


                                // =================================
                                // PARSE JSON
                                // =================================

                                EvaluationResult evaluation =
                                        parseEvaluation(
                                                rawResponse
                                        );


                                // =================================
                                // SAVE USER MESSAGE
                                // =================================

                                saveUserMessage(
                                        sessionId,
                                        cleanedUserAnswer,
                                        evaluation
                                );


                                // =================================
                                // GET AI REPLY
                                // =================================

                                String reply =
                                        evaluation.reply();


                                if (
                                        reply == null
                                                ||
                                                reply.isBlank()
                                ) {

                                    throw new RuntimeException(
                                            "AI reply is empty"
                                    );
                                }


                                reply =
                                        reply.trim();


                                // =================================
                                // SAVE AI MESSAGE
                                // =================================

                                saveAiMessage(
                                        sessionId,
                                        reply
                                );


                                // =================================
                                // LOG EVALUATION
                                // =================================

                                System.out.println(
                                        "========================================"
                                );

                                System.out.println(
                                        "📊 AI EVALUATION"
                                );

                                System.out.println(
                                        "Overall: "
                                                + evaluation.score()
                                );

                                System.out.println(
                                        "Technical: "
                                                + evaluation.technicalScore()
                                );

                                System.out.println(
                                        "Communication: "
                                                + evaluation.communicationScore()
                                );

                                System.out.println(
                                        "Problem Solving: "
                                                + evaluation.problemSolvingScore()
                                );

                                System.out.println(
                                        "Confidence: "
                                                + evaluation.confidenceScore()
                                );

                                System.out.println(
                                        "Feedback: "
                                                + evaluation.feedback()
                                );

                                System.out.println(
                                        "Reply: "
                                                + reply
                                );

                                System.out.println(
                                        "========================================"
                                );


                                // =================================
                                // SEND CLEAN REPLY TO FRONTEND
                                // =================================

                                if (
                                        onChunk != null
                                ) {

                                    try {

                                        onChunk.accept(
                                                reply
                                        );

                                    } catch (Exception e) {

                                        System.err.println(
                                                "⚠️ AI REPLY CALLBACK ERROR: "
                                                        + e.getMessage()
                                        );
                                    }
                                }


                                // =================================
                                // TTS
                                // =================================

                                Disposable ttsDisposable =
                                        groqTTService
                                                .generateSpeechChunks(

                                                        reply,

                                                        (
                                                                text,
                                                                audioUrl
                                                        ) -> {

                                                            if (
                                                                    onAudioReady != null
                                                            ) {

                                                                try {

                                                                    onAudioReady.accept(
                                                                            text,
                                                                            audioUrl
                                                                    );

                                                                } catch (Exception e) {

                                                                    System.err.println(
                                                                            "❌ TTS CALLBACK ERROR: "
                                                                                    + e.getMessage()
                                                                    );
                                                                }
                                                            }
                                                        },

                                                        () -> {

                                                            activeProcesses.remove(
                                                                    sessionId
                                                            );


                                                            if (
                                                                    onComplete != null
                                                            ) {

                                                                try {

                                                                    onComplete.run();

                                                                } catch (Exception e) {

                                                                    System.err.println(
                                                                            "❌ COMPLETE CALLBACK ERROR: "
                                                                                    + e.getMessage()
                                                                    );
                                                                }
                                                            }
                                                        },

                                                        error -> {

                                                            activeProcesses.remove(
                                                                    sessionId
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


                                activeProcesses.put(
                                        sessionId,
                                        ttsDisposable
                                );

                            } catch (Exception e) {

                                activeProcesses.remove(
                                        sessionId
                                );


                                System.err.println(
                                        "❌ CONVERSATION AI PROCESS ERROR"
                                );

                                e.printStackTrace();


                                if (
                                        onError != null
                                ) {

                                    onError.accept(
                                            e
                                    );
                                }
                            }
                        },


                        // =====================================
                        // GROQ ERROR
                        // =====================================

                        error -> {

                            activeProcesses.remove(
                                    sessionId
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


        activeProcesses.put(
                sessionId,
                disposable
        );


        return disposable;
    }


    // =========================================================
    // PARSE EVALUATION
    // =========================================================

    private EvaluationResult parseEvaluation(
            String rawResponse
    ) {

        try {

            String cleaned =
                    rawResponse
                            .replace(
                                    "```json",
                                    ""
                            )
                            .replace(
                                    "```JSON",
                                    ""
                            )
                            .replace(
                                    "```",
                                    ""
                            )
                            .trim();


            JsonNode root =
                    objectMapper.readTree(
                            cleaned
                    );


            if (
                    root == null
                            ||
                            !root.isObject()
            ) {

                throw new RuntimeException(
                        "AI response is not a JSON object"
                );
            }


            String reply =
                    root.path(
                                    "reply"
                            )
                            .asText(
                                    ""
                            )
                            .trim();


            double score =
                    clampScore(
                            root.path(
                                            "score"
                                    )
                                    .asDouble(
                                            0.0
                                    )
                    );


            double technicalScore =
                    clampScore(
                            root.path(
                                            "technicalScore"
                                    )
                                    .asDouble(
                                            0.0
                                    )
                    );


            double communicationScore =
                    clampScore(
                            root.path(
                                            "communicationScore"
                                    )
                                    .asDouble(
                                            0.0
                                    )
                    );


            double problemSolvingScore =
                    clampScore(
                            root.path(
                                            "problemSolvingScore"
                                    )
                                    .asDouble(
                                            0.0
                                    )
                    );


            double confidenceScore =
                    clampScore(
                            root.path(
                                            "confidenceScore"
                                    )
                                    .asDouble(
                                            0.0
                                    )
                    );


            String feedback =
                    root.path(
                                    "feedback"
                            )
                            .asText(
                                    ""
                            )
                            .trim();


            if (
                    reply.isBlank()
            ) {

                throw new RuntimeException(
                        "AI JSON does not contain reply"
                );
            }


            if (
                    feedback.isBlank()
            ) {

                feedback =
                        "Keep improving clarity, structure and confidence.";
            }


            return new EvaluationResult(

                    reply,

                    score,

                    technicalScore,

                    communicationScore,

                    problemSolvingScore,

                    confidenceScore,

                    feedback
            );

        } catch (Exception e) {

            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "❌ FAILED TO PARSE CONVERSATION AI JSON"
            );

            System.err.println(
                    "RAW RESPONSE:"
            );

            System.err.println(
                    rawResponse
            );

            System.err.println(
                    "========================================"
            );


            throw new RuntimeException(
                    "AI returned invalid evaluation JSON",
                    e
            );
        }
    }


    // =========================================================
    // CLAMP SCORE
    // =========================================================

    private double clampScore(
            double score
    ) {

        if (
                Double.isNaN(score)
                        ||
                        Double.isInfinite(score)
        ) {

            return 0.0;
        }


        return Math.max(
                0.0,
                Math.min(
                        100.0,
                        score
                )
        );
    }


    // =========================================================
    // CANCEL CURRENT PROCESS
    // =========================================================

    public void cancelCurrentProcess(
            Long sessionId
    ) {

        if (
                sessionId == null
        ) {

            return;
        }


        Disposable disposable =
                activeProcesses.remove(
                        sessionId
                );


        if (
                disposable != null
        ) {

            try {

                disposable.dispose();

            } catch (
                    Exception ignored
            ) {
            }
        }
    }


    // =========================================================
    // END INTERVIEW
    // =========================================================

    public EndInterviewResponse endInterview(
            Long sessionId
    ) {

        if (
                sessionId == null
        ) {

            throw new IllegalArgumentException(
                    "sessionId cannot be null"
            );
        }


        ConversationSession session =
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


        // =====================================================
        // LOAD MESSAGES
        // =====================================================

        List<ConversationMessage> messages =
                messageRepository
                        .findBySessionIdOrderByCreatedAtAsc(
                                sessionId
                        );


        // =====================================================
        // USER MESSAGES ONLY
        // =====================================================

        List<ConversationMessage> userMessages =
                messages.stream()

                        .filter(
                                message ->
                                        message != null
                                                &&
                                                message.getSender()
                                                        == MessageSender.USER
                        )

                        .filter(
                                message ->
                                        message.getScore() != null
                        )

                        .toList();


        // =====================================================
        // NO EVALUATED ANSWERS
        // =====================================================

        if (
                userMessages.isEmpty()
        ) {

            session.setStatus(
                    SessionStatus.COMPLETED
            );


            session.setEndedAt(
                    LocalDateTime.now()
            );


            session.setOverallScore(
                    0.0
            );


            session.setTechnicalScore(
                    0.0
            );


            session.setCommunicationScore(
                    0.0
            );


            session.setProblemSolvingScore(
                    0.0
            );


            session.setConfidenceScore(
                    0.0
            );


            session.setFinalFeedback(
                    "The interview was completed without enough evaluated answers to calculate a reliable performance score."
            );


            session.setTotalMessages(
                    0
            );


            sessionRepository.save(
                    session
            );


            cancelCurrentProcess(
                    sessionId
            );


            return EndInterviewResponse
                    .builder()

                    .overallScore(
                            0.0
                    )

                    .finalFeedback(
                            session.getFinalFeedback()
                    )

                    .totalMessages(
                            0
                    )

                    .build();
        }


        // =====================================================
        // OVERALL
        // =====================================================

        double overallScore =
                averageScores(
                        userMessages,
                        ScoreType.OVERALL
                );


        // =====================================================
        // TECHNICAL
        // =====================================================

        double technicalScore =
                averageScores(
                        userMessages,
                        ScoreType.TECHNICAL
                );


        // =====================================================
        // COMMUNICATION
        // =====================================================

        double communicationScore =
                averageScores(
                        userMessages,
                        ScoreType.COMMUNICATION
                );


        // =====================================================
        // PROBLEM SOLVING
        // =====================================================

        double problemSolvingScore =
                averageScores(
                        userMessages,
                        ScoreType.PROBLEM_SOLVING
                );


        // =====================================================
        // CONFIDENCE
        // =====================================================

        double confidenceScore =
                averageScores(
                        userMessages,
                        ScoreType.CONFIDENCE
                );


        // =====================================================
        // FINAL FEEDBACK
        // =====================================================

        String finalFeedback =
                generateFinalFeedback(

                        session,

                        userMessages.size(),

                        overallScore,

                        technicalScore,

                        communicationScore,

                        problemSolvingScore,

                        confidenceScore
                );


        // =====================================================
        // SAVE SESSION
        // =====================================================

        session.setStatus(
                SessionStatus.COMPLETED
        );


        session.setEndedAt(
                LocalDateTime.now()
        );


        session.setOverallScore(
                overallScore
        );


        session.setTechnicalScore(
                technicalScore
        );


        session.setCommunicationScore(
                communicationScore
        );


        session.setProblemSolvingScore(
                problemSolvingScore
        );


        session.setConfidenceScore(
                confidenceScore
        );


        session.setFinalFeedback(
                finalFeedback
        );


        session.setTotalMessages(
                userMessages.size()
        );


        sessionRepository.save(
                session
        );


        cancelCurrentProcess(
                sessionId
        );


        return EndInterviewResponse
                .builder()

                .overallScore(
                        overallScore
                )

                .finalFeedback(
                        finalFeedback
                )

                .totalMessages(
                        userMessages.size()
                )

                .build();
    }


    // =========================================================
    // AVERAGE SCORES
    // =========================================================

    private double averageScores(

            List<ConversationMessage> messages,

            ScoreType scoreType

    ) {

        if (
                messages == null
                        ||
                        messages.isEmpty()
        ) {

            return 0.0;
        }


        double total =
                0.0;


        int count =
                0;


        for (
                ConversationMessage message :
                messages
        ) {

            if (
                    message == null
            ) {

                continue;
            }


            Double value =
                    null;


            switch (
                    scoreType
            ) {

                case OVERALL:

                    value =
                            message.getScore();

                    break;


                case TECHNICAL:

                    value =
                            message.getTechnicalScore();

                    break;


                case COMMUNICATION:

                    value =
                            message.getCommunicationScore();

                    break;


                case PROBLEM_SOLVING:

                    value =
                            message.getProblemSolvingScore();

                    break;


                case CONFIDENCE:

                    value =
                            message.getConfidenceScore();

                    break;
            }


            if (
                    value == null
            ) {

                continue;
            }


            total +=
                    clampScore(
                            value
                    );


            count++;
        }


        if (
                count == 0
        ) {

            return 0.0;
        }


        return round(
                total /
                        count
        );
    }


    // =========================================================
    // FINAL FEEDBACK
    // =========================================================

    private String generateFinalFeedback(

            ConversationSession session,

            int totalMessages,

            double overallScore,

            double technicalScore,

            double communicationScore,

            double problemSolvingScore,

            double confidenceScore

    ) {

        String modeText =
                session.getMode()
                        == InterviewMode.HR
                        ? "HR interview"
                        : "communication practice";


        List<String> improvements =
                new ArrayList<>();


        if (
                technicalScore < 70
        ) {

            improvements.add(
                    "technical and professional understanding"
            );
        }


        if (
                communicationScore < 70
        ) {

            improvements.add(
                    "English communication and clarity"
            );
        }


        if (
                problemSolvingScore < 70
        ) {

            improvements.add(
                    "problem solving and logical reasoning"
            );
        }


        if (
                confidenceScore < 70
        ) {

            improvements.add(
                    "confidence and answer completeness"
            );
        }


        String improvementText;


        if (
                improvements.isEmpty()
        ) {

            improvementText =
                    "Your performance was well balanced across the evaluated areas. Keep practicing to maintain consistency.";

        } else {

            improvementText =
                    "The main areas to work on are "
                            +
                            String.join(
                                    ", ",
                                    improvements
                            )
                            +
                            ".";
        }


        return
                "Your "
                        +
                        modeText
                        +
                        " is completed with an overall score of "
                        +
                        formatScore(
                                overallScore
                        )
                        +
                        "/100 across "
                        +
                        totalMessages
                        +
                        " evaluated answers. "
                        +
                        "Technical: "
                        +
                        formatScore(
                                technicalScore
                        )
                        +
                        ", Communication: "
                        +
                        formatScore(
                                communicationScore
                        )
                        +
                        ", Problem Solving: "
                        +
                        formatScore(
                                problemSolvingScore
                        )
                        +
                        ", Confidence: "
                        +
                        formatScore(
                                confidenceScore
                        )
                        +
                        ". "
                        +
                        improvementText;
    }


    // =========================================================
    // FORMAT SCORE
    // =========================================================

    private String formatScore(
            double score
    ) {

        return String.format(
                "%.1f",
                clampScore(
                        score
                )
        );
    }


    // =========================================================
    // ROUND
    // =========================================================

    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }


    // =========================================================
    // FAIL
    // =========================================================

    private Disposable fail(

            Consumer<Throwable> onError,

            Throwable throwable

    ) {

        if (
                onError != null
        ) {

            onError.accept(
                    throwable
            );
        }


        return noOp();
    }


    // =========================================================
    // NO OP
    // =========================================================

    private Disposable noOp() {

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


    // =========================================================
    // EVALUATION RESULT
    // =========================================================

    private record EvaluationResult(

            String reply,

            Double score,

            Double technicalScore,

            Double communicationScore,

            Double problemSolvingScore,

            Double confidenceScore,

            String feedback

    ) {
    }


    // =========================================================
    // SCORE TYPE
    // =========================================================

    private enum ScoreType {

        OVERALL,

        TECHNICAL,

        COMMUNICATION,

        PROBLEM_SOLVING,

        CONFIDENCE
    }
}