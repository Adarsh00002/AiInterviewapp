package com.example.MyFirstApp.service.AiCoachGroq;

import com.example.MyFirstApp.DTO.GroqDTO.GroqRequest;
import com.example.MyFirstApp.DTO.GroqDTO.GroqResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCoachGroqService {

    private final WebClient webClient;


    // =========================================================
    // API KEY
    // =========================================================

    @Value("${GROQ_APIKEY}")
    private String apiKey;


    // =========================================================
    // GROQ
    // =========================================================

    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";


    private static final String MODEL =
            "openai/gpt-oss-120b";


    // =========================================================
    // ASK AI
    // =========================================================

    public String askAI(
            String prompt
    ) {

        if (
                prompt == null
                        ||
                        prompt.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Prompt cannot be empty"
            );
        }


        GroqRequest request =
                new GroqRequest(

                        MODEL,

                        List.of(
                                new GroqRequest.Message(
                                        "user",
                                        prompt
                                )
                        ),

                        false
                );


        System.out.println(
                "========================================"
        );

        System.out.println(
                "🤖 AI COACH GROQ REQUEST"
        );

        System.out.println(
                "MODEL: " + MODEL
        );

        System.out.println(
                "========================================"
        );


        GroqResponse response =
                webClient
                        .post()

                        .uri(
                                GROQ_URL
                        )

                        .header(
                                "Authorization",
                                "Bearer " + apiKey
                        )

                        .contentType(
                                MediaType.APPLICATION_JSON
                        )

                        .bodyValue(
                                request
                        )

                        .retrieve()

                        .bodyToMono(
                                GroqResponse.class
                        )

                        .block();


        if (
                response == null
                        ||
                        response.getChoices() == null
                        ||
                        response.getChoices().isEmpty()
                        ||
                        response.getChoices().get(0)
                                .getMessage() == null
        ) {

            throw new RuntimeException(
                    "No response received from Groq"
            );
        }


        String result =
                response
                        .getChoices()
                        .get(0)
                        .getMessage()
                        .getContent();


        if (
                result == null
                        ||
                        result.isBlank()
        ) {

            throw new RuntimeException(
                    "Groq returned empty response"
            );
        }


        return result.trim();
    }
}