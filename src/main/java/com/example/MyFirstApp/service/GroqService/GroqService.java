
        package com.example.MyFirstApp.service.GroqService;



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
public class GroqService {

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

        System.out.println("========== GROQ DEBUG ==========");
        System.out.println("GROQ URL = " + GROQ_URL);
        System.out.println("MODEL = " + MODEL);
        System.out.println("API KEY PRESENT = " + (apiKey != null && !apiKey.isBlank()));
        System.out.println("API KEY LENGTH = " + (apiKey == null ? 0 : apiKey.length()));
        System.out.println("================================");
        System.out.println(
                "GROQ REQUEST = " + request
        );
        GroqResponse response =
                webClient.post()
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
                    "No response received from Groq AI."
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
    //
    // IMPORTANT:
    //
    // Ye method ab Disposable return karta hai.
    //
    // Isliye LiveInterviewService:
    //
    // disposable.dispose()
    //
    // karke current Groq stream cancel kar sakta hai.
    //
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
                "🤖 GROQ STREAM STARTED"
        );

        System.out.println(
                "MODEL: "
                        + MODEL
        );

        System.out.println(
                "THREAD: "
                        + Thread.currentThread().getName()
        );

        System.out.println(
                "PROMPT:"
        );

        System.out.println(
                prompt
        );

        System.out.println(
                "========================================"
        );


        // =====================================================
        // STREAM
        // =====================================================

        return webClient
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
                .accept(
                        MediaType.TEXT_EVENT_STREAM
                )
                .bodyValue(
                        request
                )
                .retrieve()

                // =================================================
                // READ SSE
                // =================================================

                .bodyToFlux(
                        new ParameterizedTypeReference<
                                ServerSentEvent<String>
                                >() {
                        }
                )

                // =================================================
                // GET DATA
                // =================================================

                .map(
                        ServerSentEvent::data
                )

                // =================================================
                // IGNORE EMPTY / DONE
                // =================================================

                .filter(
                        data ->
                                data != null
                                        && !data.isBlank()
                                        && !data.equals("[DONE]")
                )

                // =================================================
                // PARSE GROQ JSON
                // =================================================

                .flatMap(
                        data -> {

                            try {

                                System.out.println(
                                        "📩 GROQ RAW SSE:"
                                );

                                System.out.println(
                                        data
                                );


                                JsonNode json =
                                        objectMapper.readTree(
                                                data
                                        );


                                JsonNode choices =
                                        json.path(
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
                                                .path(
                                                        "delta"
                                                );


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


                                System.out.println(
                                        "🤖 GROQ CHUNK:"
                                                + text
                                );


                                return Flux.just(
                                        text
                                );

                            } catch (Exception e) {

                                System.err.println(
                                        "❌ GROQ SSE PARSE ERROR"
                                );

                                System.err.println(
                                        "RAW DATA:"
                                );

                                System.err.println(
                                        data
                                );

                                System.err.println(
                                        "MESSAGE: "
                                                + e.getMessage()
                                );

                                return Flux.empty();
                            }
                        }
                )

                // =================================================
                // SEND CHUNK
                // =================================================

                .doOnNext(
                        chunk -> {

                            System.out.println(
                                    "📤 SENDING AI CHUNK:"
                            );

                            System.out.println(
                                    chunk
                            );


                            if (
                                    onChunk != null
                            ) {

                                try {

                                    onChunk.accept(
                                            chunk
                                    );

                                } catch (Exception callbackError) {

                                    System.err.println(
                                            "❌ AI CHUNK CALLBACK ERROR: "
                                                    + callbackError.getMessage()
                                    );
                                }
                            }
                        }
                )

                // =================================================
                // COMPLETE
                // =================================================

                .doOnComplete(
                        () -> {

                            System.out.println(
                                    "========================================"
                            );

                            System.out.println(
                                    "✅ GROQ STREAM COMPLETED"
                            );

                            System.out.println(
                                    "========================================"
                            );


                            if (
                                    onComplete != null
                            ) {

                                try {

                                    onComplete.run();

                                } catch (Exception callbackError) {

                                    System.err.println(
                                            "❌ AI COMPLETE CALLBACK ERROR: "
                                                    + callbackError.getMessage()
                                    );
                                }
                            }
                        }
                )

                // =================================================
                // ERROR
                // =================================================

                .doOnError(
                        error -> {

                            System.err.println(
                                    "========================================"
                            );

                            System.err.println(
                                    "❌ GROQ STREAM ERROR"
                            );

                            System.err.println(
                                    "TYPE: "
                                            + error.getClass().getName()
                            );

                            System.err.println(
                                    "MESSAGE: "
                                            + error.getMessage()
                            );

                            System.err.println(
                                    "========================================"


                            );

                            if (
                                    onError != null
                            ) {

                                try {

                                    onError.accept(
                                            error
                                    );

                                } catch (Exception callbackError) {

                                    System.err.println(
                                            "❌ AI ERROR CALLBACK ERROR: "
                                                    + callbackError.getMessage()
                                    );
                                }
                            }
                        }
                )

                // =================================================
                // SUBSCRIBE
                // =================================================

                .subscribe();
    }
}

