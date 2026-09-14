package com.example.MyFirstApp.service.HRandCommunication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ConversationGroqService {

    private final WebClient webClient;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Value("${GROQ_APIKEY}")
    private String groqApiKey;

    /*
     * Conversation ke liye alag model.
     *
     * Agar tumhara existing Resume service kisi aur model
     * ka use karta hai to usse koi relation nahi rahega.
     */
    @Value("${conversation.groq.model:openai/gpt-oss-20b}")
    private String model;


    // =========================================================
    // ASK AI - NON STREAMING
    // =========================================================

    public String askAI(String prompt) {

        try {

            Map<String, Object> request =
                    Map.of(
                            "model",
                            model,

                            "messages",
                            new Object[]{
                                    Map.of(
                                            "role",
                                            "user",
                                            "content",
                                            prompt
                                    )
                            },

                            "temperature",
                            0.4
                    );


            JsonNode response =
                    webClient
                            .post()
                            .uri(
                                    "https://api.groq.com/openai/v1/chat/completions"
                            )
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + groqApiKey
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .bodyValue(
                                    request
                            )
                            .retrieve()
                            .bodyToMono(
                                    JsonNode.class
                            )
                            .block();


            if (
                    response == null
                            ||
                            !response.has("choices")
            ) {

                throw new RuntimeException(
                        "Empty response from Conversation Groq"
                );
            }


            JsonNode content =
                    response
                            .path("choices")
                            .path(0)
                            .path("message")
                            .path("content");


            if (
                    content.isMissingNode()
                            ||
                            content.isNull()
            ) {

                throw new RuntimeException(
                        "Conversation Groq response content missing"
                );
            }


            return content.asText().trim();


        } catch (Exception e) {

            System.err.println(
                    "❌ CONVERSATION GROQ ERROR: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Conversation Groq request failed",
                    e
            );
        }
    }


    // =========================================================
    // STREAM AI
    // =========================================================

    public Disposable streamAI(

            String prompt,

            Consumer<String> onChunk,

            Runnable onComplete,

            Consumer<Throwable> onError

    ) {

        try {

            Map<String, Object> request =
                    Map.of(
                            "model",
                            model,

                            "messages",
                            new Object[]{
                                    Map.of(
                                            "role",
                                            "user",
                                            "content",
                                            prompt
                                    )
                            },

                            "temperature",
                            0.4,

                            "stream",
                            true
                    );


            Flux<String> responseFlux =
                    webClient
                            .post()
                            .uri(
                                    "https://api.groq.com/openai/v1/chat/completions"
                            )
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + groqApiKey
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
                                    String.class
                            );


            return responseFlux.subscribe(

                    data -> {

                        try {

                            processStreamData(
                                    data,
                                    onChunk
                            );

                        } catch (Exception e) {

                            System.err.println(
                                    "⚠️ CONVERSATION GROQ CHUNK ERROR: "
                                            + e.getMessage()
                            );
                        }
                    },

                    error -> {

                        System.err.println(
                                "❌ CONVERSATION GROQ STREAM ERROR: "
                                        + error.getMessage()
                        );


                        if (
                                onError != null
                        ) {

                            onError.accept(
                                    error
                            );
                        }
                    },

                    () -> {

                        System.out.println(
                                "✅ CONVERSATION GROQ STREAM COMPLETE"
                        );


                        if (
                                onComplete != null
                        ) {

                            onComplete.run();
                        }
                    }
            );


        } catch (Exception e) {

            System.err.println(
                    "❌ CONVERSATION GROQ STREAM START ERROR: "
                            + e.getMessage()
            );


            if (
                    onError != null
            ) {

                onError.accept(
                        e
                );
            }


            return () -> {};
        }
    }


    // =========================================================
    // PROCESS STREAM DATA
    // =========================================================

    private void processStreamData(

            String data,

            Consumer<String> onChunk

    ) {

        if (
                data == null
                        ||
                        data.isBlank()
        ) {

            return;
        }


        String[] lines =
                data.split("\\r?\\n");


        for (
                String line : lines
        ) {

            line =
                    line.trim();


            if (
                    line.isEmpty()
            ) {

                continue;
            }


            if (
                    line.startsWith("data:")
            ) {

                line =
                        line.substring(
                                5
                        ).trim();
            }


            if (
                    "[DONE]".equals(line)
            ) {

                continue;
            }


            try {

                JsonNode root =
                        objectMapper.readTree(
                                line
                        );


                JsonNode content =
                        root
                                .path("choices")
                                .path(0)
                                .path("delta")
                                .path("content");


                if (
                        !content.isMissingNode()
                                &&
                                !content.isNull()
                ) {

                    String text =
                            content.asText();


                    if (
                            text != null
                                    &&
                                    !text.isEmpty()
                    ) {

                        if (
                                onChunk != null
                        ) {

                            onChunk.accept(
                                    text
                            );
                        }
                    }
                }


            } catch (Exception ignored) {

                /*
                 * SSE data kabhi-kabhi partial chunk me aa sakta hai.
                 * Isliye malformed individual chunk ko ignore kar rahe hain.
                 */
            }
        }
    }
}