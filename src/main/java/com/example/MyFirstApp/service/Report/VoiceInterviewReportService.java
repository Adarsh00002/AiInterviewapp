package com.example.MyFirstApp.service.Report;

import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewQuestion;
import com.example.MyFirstApp.Entity.voiceInterviewEntity.VoiceInterviewSession;
import com.example.MyFirstApp.repository.voiceInterviewRepository.VoiceInterviewQuestionRepository;
import com.example.MyFirstApp.repository.voiceInterviewRepository.VoiceInterviewSessionRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoiceInterviewReportService {

    private final VoiceInterviewSessionRepository sessionRepository;
    private final VoiceInterviewQuestionRepository questionRepository;


    public byte[] generateReport(Long sessionId) {

        VoiceInterviewSession session =
                sessionRepository.findBySessionId(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Interview session not found"
                                )
                        );


        List<VoiceInterviewQuestion> questions =
                questionRepository.findBySessionId(sessionId);


        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document =
                    new Document(PageSize.A4, 40, 40, 40, 40);

            PdfWriter.getInstance(
                    document,
                    outputStream
            );


            document.open();


            // =====================================================
            // TITLE
            // =====================================================

            Font titleFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            22
                    );

            Paragraph title =
                    new Paragraph(
                            "AI Voice Interview Report",
                            titleFont
                    );

            title.setAlignment(Element.ALIGN_CENTER);

            document.add(title);

            document.add(
                    new Paragraph(" ")
            );


            // =====================================================
            // BASIC INFORMATION
            // =====================================================

            Font headingFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            15
                    );

            Font normalFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            11
                    );


            document.add(
                    new Paragraph(
                            "Interview Summary",
                            headingFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Session ID: " + sessionId,
                            normalFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Total Questions: "
                                    + questions.size(),
                            normalFont
                    )
            );


            // =====================================================
            // SCORE
            // =====================================================

            document.add(
                    new Paragraph(" ",
                            normalFont)
            );

            document.add(
                    new Paragraph(
                            "Overall Performance",
                            headingFont
                    )
            );


            document.add(
                    new Paragraph(
                            "Overall Score: "
                                    + getOverallScore(questions)
                                    + "/100",
                            normalFont
                    )
            );


            // =====================================================
            // QUESTIONS
            // =====================================================

            document.add(
                    new Paragraph(" ",
                            normalFont)
            );

            document.add(
                    new Paragraph(
                            "Detailed Review",
                            headingFont
                    )
            );


            int questionNumber = 1;


            for (VoiceInterviewQuestion question : questions) {

                document.add(
                        new Paragraph(
                                "Q"
                                        + questionNumber
                                        + ". "
                                        + safe(question.getQuestion()),
                                FontFactory.getFont(
                                        FontFactory.HELVETICA_BOLD,
                                        12
                                )
                        )
                );


                document.add(
                        new Paragraph(
                                "Answer: "
                                        + safe(question.getAnswer()),
                                normalFont
                        )
                );


                document.add(
                        new Paragraph(
                                "Score: "
                                        + question.getScore()
                                        + "/10",
                                normalFont
                        )
                );


                document.add(
                        new Paragraph(
                                "Feedback: "
                                        + safe(question.getFeedback()),
                                normalFont
                        )
                );


                document.add(
                        new Paragraph(
                                " "
                        )
                );


                questionNumber++;
            }


            // =====================================================
            // END
            // =====================================================

            document.add(
                    new Paragraph(
                            "Generated by AI Interview Coach",
                            FontFactory.getFont(
                                    FontFactory.HELVETICA_OBLIQUE,
                                    9
                            )
                    )
            );


            document.close();


            return outputStream.toByteArray();


        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate interview report",
                    e
            );
        }
    }


    private String safe(String value) {

        if (value == null || value.isBlank()) {
            return "Not available";
        }

        return value;
    }


    private double getOverallScore(
            List<VoiceInterviewQuestion> questions
    ) {

        if (questions == null || questions.isEmpty()) {
            return 0;
        }


        double total = 0;


        for (VoiceInterviewQuestion question : questions) {

            if (question.getScore() != null) {
                total += question.getScore();
            }
        }


        double average =
                total / questions.size();


        return Math.round(
                average * 10
        );
    }
}