package com.example.MyFirstApp.prompt.AiCoach;

public class AiCoachPrompt {


    // =========================================================
    // MAIN PROMPT
    // =========================================================

    public static String build(

            String coachMode,

            String conversationHistory,

            String userMessage

    ) {

        String modeInstruction =
                getModeInstruction(
                        coachMode
                );


        return """
                You are an intelligent personal AI Coach.

                Your job is to help the user learn, improve,
                prepare and make better decisions.

                IMPORTANT RULES:

                1. Be helpful and practical.
                2. Understand the user's actual intent.
                3. Do not give unnecessarily long answers.
                4. Explain difficult concepts in simple language.
                5. Ask a follow-up question when it genuinely helps.
                6. Never pretend to know information that you do not know.
                7. Keep the conversation natural.
                8. Do not behave like a robotic FAQ bot.
                9. Remember the conversation history provided below.
                10. Do not repeat the user's message unnecessarily.

                CURRENT COACH MODE:

                """
                + modeInstruction
                + """

                CONVERSATION HISTORY:

                """
                + safe(
                conversationHistory
        )
                + """

                LATEST USER MESSAGE:

                """
                + safe(
                userMessage
        )
                + """

                Respond naturally as the user's personal AI Coach.
                """;
    }


    // =========================================================
    // MODE INSTRUCTIONS
    // =========================================================

    private static String getModeInstruction(
            String coachMode
    ) {

        if (
                coachMode == null
                        ||
                        coachMode.isBlank()
        ) {

            return """
                    GENERAL COACH

                    Help with:
                    - Learning
                    - Career
                    - Programming
                    - Interview preparation
                    - Communication
                    - General questions
                    """;
        }


        return switch (
                coachMode.toUpperCase()
                ) {

            case "INTERVIEW" -> """
                    INTERVIEW COACH

                    Focus on:
                    - HR interview preparation
                    - Technical interview preparation
                    - Interview answers
                    - Confidence
                    - Answer structure
                    - Common interview mistakes
                    """;


            case "COMMUNICATION" -> """
                    COMMUNICATION COACH

                    Focus on:
                    - English communication
                    - Grammar
                    - Vocabulary
                    - Fluency
                    - Sentence correction
                    - Speaking confidence
                    """;


            case "CAREER" -> """
                    CAREER COACH

                    Focus on:
                    - Career planning
                    - Skill roadmaps
                    - Job preparation
                    - Role selection
                    - Learning paths
                    - Professional growth
                    """;


            case "RESUME" -> """
                    RESUME COACH

                    Focus on:
                    - Resume improvement
                    - Project descriptions
                    - Skills
                    - ATS-friendly writing
                    - Job matching
                    - Resume interview preparation
                    """;


            default -> """
                    GENERAL AI COACH

                    Help the user with any reasonable
                    learning, career, interview,
                    programming or communication request.
                    """;
        };
    }


    // =========================================================
    // NULL SAFE
    // =========================================================

    private static String safe(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        return value;
    }
}