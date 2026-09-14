package com.example.MyFirstApp.prompt.ResumeInterviewPromt;


public class ResumeAnalysisPrompt {

    private ResumeAnalysisPrompt() {
    }

    public static String build(String resumeText) {

        return """
                You are an expert resume analyst and technical recruiter.

                Analyze the following candidate resume deeply.

                Your goal is to create a structured profile that will
                later be used to conduct a personalized technical
                interview.

                RESUME:
                -------------------------
                %s
                -------------------------

                Analyze the resume and return ONLY valid JSON.

                Required JSON structure:

                {
                  "resumeScore": 0,
                  "overallSummary": "",
                  "background": "",

                  "skills": [
                    ""
                  ],

                  "projects": [
                    {
                      "title": "",
                      "description": "",
                      "technologies": [
                        ""
                      ],
                      "projectImpact": ""
                    }
                  ],

                  "strengths": [
                    ""
                  ],

                  "improvements": [
                    ""
                  ],

                  "interviewFocus": [
                    ""
                  ]
                }

                Rules:

                1. resumeScore must be an integer from 0 to 100.

                2. Detect only skills that are actually present
                   or strongly supported by the resume.

                3. Extract all meaningful technical projects.

                4. For each project identify:
                   - project title
                   - what the project does
                   - technologies used
                   - technical impact or contribution

                5. background should summarize:
                   - education
                   - experience
                   - roles
                   - career background

                6. strengths should identify actual resume strengths.

                7. improvements should identify realistic weaknesses
                   or missing information.

                8. interviewFocus should identify the most important
                   topics that should be questioned during interview.

                9. Do not invent technologies, projects or experience.

                10. Do not return markdown.

                11. Return JSON only.
                """.formatted(resumeText);
    }
}