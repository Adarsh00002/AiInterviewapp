package com.example.MyFirstApp.prompt.QuickDrill;


public class QuickDrillPrompt {

    private QuickDrillPrompt() {
    }


    // =========================================================
    // FIRST QUESTION
    // =========================================================

    public static String buildFirstQuestionPrompt(
            String topics,
            String difficulty,
            int questionNumber
    ) {

        return """
                You are an expert technical interviewer conducting a quick technical drill.

                Topics:
                %s

                Difficulty:
                %s

                Question number:
                %d

                Generate ONE technical interview question.

                Rules:
                - Ask only one question.
                - Do not answer the question.
                - Do not give hints.
                - Do not use markdown.
                - Do not use bullets.
                - Keep the question concise.
                - The question must test actual technical understanding.
                - Match the requested difficulty.
                - Use one of the provided topics.

                Return ONLY the question text.
                """.formatted(
                topics,
                difficulty,
                questionNumber
        );
    }


    // =========================================================
    // NEXT QUESTION
    // =========================================================

    public static String buildNextQuestionPrompt(
            String topics,
            String difficulty,
            String previousQuestion,
            String previousAnswer,
            int questionNumber
    ) {

        return """
                You are an expert technical interviewer conducting a quick technical drill.

                Topics:
                %s

                Difficulty:
                %s

                Previous question:
                %s

                Candidate answer:
                %s

                Next question number:
                %d

                Generate ONE new technical interview question.

                Rules:
                - Ask exactly ONE question.
                - Do not repeat the previous question.
                - Build naturally from the previous answer when useful.
                - Stay within the selected topics.
                - Match the requested difficulty.
                - Do not answer the question.
                - No markdown.
                - No bullets.
                - Keep it concise.

                Return ONLY the question text.
                """.formatted(
                topics,
                difficulty,
                previousQuestion,
                previousAnswer,
                questionNumber
        );
    }


    // =========================================================
    // ANSWER EVALUATION
    // =========================================================

    public static String buildEvaluationPrompt(
            String topic,
            String difficulty,
            String question,
            String answer
    ) {

        return """
                You are an expert technical interviewer evaluating a candidate's answer.

                Topic:
                %s

                Difficulty:
                %s

                Question:
                %s

                Candidate answer:
                %s

                Evaluate the answer fairly.

                Return ONLY valid JSON using exactly this structure:

                {
                  "score": 0,
                  "whatYouDidWell": [
                    ""
                  ],
                  "whatToImprove": [
                    ""
                  ],
                  "idealAnswer": ""
                }

                Rules:

                1. score must be a number from 0 to 10.
                2. Evaluate technical correctness.
                3. Evaluate completeness.
                4. Evaluate technical depth.
                5. Evaluate clarity.
                6. Do not reward irrelevant content.
                7. Do not invent information.
                8. whatYouDidWell should contain specific points.
                9. whatToImprove should contain specific points.
                10. idealAnswer should be technically correct and concise.
                11. Do not use markdown.
                12. Return JSON only.

                """.formatted(
                topic,
                difficulty,
                question,
                answer
        );
    }
}

