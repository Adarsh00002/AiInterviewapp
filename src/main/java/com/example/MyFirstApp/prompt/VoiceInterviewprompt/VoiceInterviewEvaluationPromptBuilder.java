package com.example.MyFirstApp.prompt.VoiceInterviewprompt;

import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewSession;

public class VoiceInterviewEvaluationPromptBuilder {

    private VoiceInterviewEvaluationPromptBuilder() {
    }

    public static String build(
            VoiceInterviewSession session,
            String question,
            String answer
    ) {

        return """
                You are an expert technical interview evaluator.

                Evaluate the candidate answer objectively based only on the
                question, candidate answer, role, experience and practice type.

                Candidate Role:
                %s

                Practice Type:
                %s

                Experience:
                %s

                Interview Question:
                %s

                Candidate Answer:
                %s

                =========================================================
                EVALUATION DIMENSIONS
                =========================================================

                Evaluate these four dimensions independently:

                1. Communication
                2. Technical Knowledge
                3. Problem Solving
                4. Confidence & Clarity

                Also calculate one overall score.

                =========================================================
                SCORING SCALE
                =========================================================

                Every score MUST be an INTEGER from 0 to 10.

                10 = Exceptional
                9  = Excellent
                8  = Very Good
                7  = Good
                6  = Above Average
                5  = Average
                4  = Below Average
                3  = Weak
                2  = Very Weak
                1  = Extremely Weak
                0  = No meaningful answer

                IMPORTANT:

                - Never return scores from 0 to 100.
                - Never return decimal scores.
                - Never return negative scores.
                - All five scores must be integers between 0 and 10.
                - Score the actual answer, not the candidate's potential.
                - Do not give a high score merely because the answer is long.
                - Do not give a low score merely because the answer is short.
                - Technical correctness is more important than verbosity.
                - Consider the candidate's experience level.
                - Do not assume information that the candidate did not provide.

                =========================================================
                OVERALL SCORE
                =========================================================

                The overall score should represent the candidate's complete
                performance for this particular answer.

                Use the four dimensions to determine the overall score.

                Overall score should normally be close to the balanced average
                of the four dimensions, but you may adjust it slightly when
                one dimension is clearly critical to the answer.

                =========================================================
                FEEDBACK
                =========================================================

                Feedback must be practical and specific.

                Include:

                - What the candidate did well.
                - What was missing.
                - Important technical mistakes, if any.
                - How the answer could be improved.
                - One clear practical suggestion.

                Maximum 120 words.

                =========================================================
                SKILL
                =========================================================

                Select the PRIMARY skill most relevant to evaluating this answer.

                Examples:

                Java
                Spring Boot
                Spring Security
                REST API
                Database
                SQL
                System Design
                Data Structures
                Algorithms
                Problem Solving
                Communication
                Leadership
                Authentication
                Networking
                OOP
                etc.

                =========================================================
                OUTPUT
                =========================================================

                Return ONLY valid JSON.

                {
                  "score": 0,
                  "communicationScore": 0,
                  "technicalKnowledgeScore": 0,
                  "problemSolvingScore": 0,
                  "confidenceClarityScore": 0,
                  "feedback": "Personalized feedback",
                  "skill": "Primary skill"
                }

                =========================================================
                STRICT JSON RULES
                =========================================================

                - All score fields must be INTEGER values.
                - All score fields must be between 0 and 10.
                - Do not use strings for scores.
                - Do not return percentages.
                - Do not return markdown.
                - Do not return ```json.
                - Do not add explanation outside JSON.
                - Do not add extra JSON fields.
                """.formatted(
                safe(session != null
                        ? session.getSelectedRole()
                        : null),

                safe(session != null
                        ? session.getPracticeType()
                        : null),

                safe(session != null
                        ? session.getTotalExperience()
                        : null),

                safe(question),

                safe(answer)
        );
    }

    private static String safe(String value) {

        if (value == null || value.isBlank()) {
            return "Not provided";
        }

        return value.trim();
    }
}