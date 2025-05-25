package com.example.gptbot.controller;

import com.example.gptbot.model.Request;
import com.example.gptbot.model.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestController
@RequestMapping("/api/chat")
public class GptController {

    private final WebClient webClient;
    private final String apiKey;

    public GptController(@Value("${openai.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @PostMapping
    public Mono<Response> chat(@RequestBody Request request) {
        String userMessage = request.getMessage();

        String requestJson = """
                {
                  "model": "gpt-3.5-turbo",
                  "messages": [{"role": "user", "content": "%s"}]
                }
                """.formatted(userMessage);

        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests))
                .map(response -> new Response("AI response: " + trim(response)));
    }

    private String trim(String response) {
        return response.length() > 300 ? response.substring(0, 300) + "..." : response;
    }
}
