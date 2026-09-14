package com.example.MyFirstApp.service.ResumeGroqService;

import com.example.MyFirstApp.DTO.GroqDTO.GroqRequest;
import com.example.MyFirstApp.DTO.GroqDTO.GroqResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ResumeGroqService {

    private final WebClient webClient;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Value("${GROQ_APIKEY}")
    private String apiKey;

    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    private static final String MODEL =
            "openai/gpt-oss-120b";


    // =========================================================
    // NORMAL AI RESPONSE
    // =========================================================

    public String askAI(
            String prompt
    ) {

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
                "📄 RESUME GROQ REQUEST"
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
                        .uri(GROQ_URL)
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
                        || response.getChoices() == null
                        || response.getChoices().isEmpty()
                        || response.getChoices().get(0).getMessage() == null
        ) {

            throw new RuntimeException(
                    "No response received from Groq"
            );
        }

        return response
                .getChoices()
                .get(0)
                .getMessage()
                .getContent()
                .trim();
    }


    // =========================================================
    // STREAMING AI
    // =========================================================

    public Disposable streamAI(

            String prompt,

            Consumer<String> onChunk,

            Runnable onComplete,

            Consumer<Throwable> onError

    ) {

        GroqRequest request =
                new GroqRequest(
                        MODEL,
                        List.of(
                                new GroqRequest.Message(
                                        "user",
                                        prompt
                                )
                        ),
                        true
                );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "🎯 RESUME LIVE GROQ STREAM STARTED"
        );

        System.out.println(
                "MODEL: " + MODEL
        );

        System.out.println(
                "========================================"
        );

        return webClient
                .post()
                .uri(GROQ_URL)
                .header(
                        "Authorization",
                        "Bearer " + apiKey
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .accept(
                        MediaType.TEXT_EVENT_STREAM
                )
                .bodyValue(
                        request
                )
                .retrieve()
                .bodyToFlux(
                        new ParameterizedTypeReference<
                                ServerSentEvent<String>
                                >() {}
                )
                .map(
                        ServerSentEvent::data
                )
                .filter(
                        data ->
                                data != null
                                        && !data.isBlank()
                                        && !data.equals("[DONE]")
                )
                .flatMap(
                        data -> {

                            try {

                                JsonNode root =
                                        objectMapper.readTree(
                                                data
                                        );

                                JsonNode choices =
                                        root.path(
                                                "choices"
                                        );

                                if (
                                        !choices.isArray()
                                                || choices.isEmpty()
                                ) {
                                    return Flux.empty();
                                }

                                JsonNode delta =
                                        choices
                                                .get(0)
                                                .path("delta");

                                JsonNode content =
                                        delta.path(
                                                "content"
                                        );

                                if (
                                        content.isMissingNode()
                                                || content.isNull()
                                ) {
                                    return Flux.empty();
                                }

                                String text =
                                        content.asText();

                                if (
                                        text == null
                                                || text.isBlank()
                                ) {
                                    return Flux.empty();
                                }

                                return Flux.just(
                                        text
                                );

                            } catch (Exception e) {

                                System.err.println(
                                        "❌ RESUME GROQ SSE PARSE ERROR: "
                                                + e.getMessage()
                                );

                                return Flux.empty();
                            }
                        }
                )
                .doOnNext(
                        chunk -> {

                            if (onChunk != null) {

                                try {

                                    onChunk.accept(
                                            chunk
                                    );

                                } catch (Exception e) {

                                    System.err.println(
                                            "❌ RESUME AI CHUNK CALLBACK ERROR: "
                                                    + e.getMessage()
                                    );
                                }
                            }
                        }
                )
                .doOnComplete(
                        () -> {

                            System.out.println(
                                    "✅ RESUME GROQ STREAM COMPLETED"
                            );

                            if (onComplete != null) {

                                try {

                                    onComplete.run();

                                } catch (Exception e) {

                                    System.err.println(
                                            "❌ RESUME AI COMPLETE CALLBACK ERROR: "
                                                    + e.getMessage()
                                    );
                                }
                            }
                        }
                )
                .doOnError(
                        error -> {

                            System.err.println(
                                    "❌ RESUME GROQ STREAM ERROR: "
                                            + (
                                            error == null
                                                    ? "Unknown error"
                                                    : error.getMessage()
                                    )
                            );

                            if (onError != null) {

                                try {

                                    onError.accept(
                                            error
                                    );

                                } catch (Exception e) {

                                    System.err.println(
                                            "❌ RESUME AI ERROR CALLBACK ERROR: "
                                                    + e.getMessage()
                                    );
                                }
                            }
                        }
                )
                .subscribe();
    }
}